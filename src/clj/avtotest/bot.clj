(ns avtotest.bot
  (:require [morse.api :as t]
            [avtotest.db :as db]
            [clojure.edn :as edn]
            [mount.core :as mount]
            [avtotest.util :as util]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [avtotest.query :as query]
            [clj-http.client :as http]
            [morse.handlers :refer :all]
            [avtotest.config :refer [env]]
            [cheshire.core :refer [generate-string]]))


(def select-all-text "Барча мавзулар")


(mount/defstate questions
  :start (-> (io/resource "questions.edn")
             (slurp)
             (edn/read-string)))


(mount/defstate sections
  :start (->> questions
              (map (juxt :section :sub-section))
              (set)
              (reduce
                (fn [a [section sub-section]]
                  (update a section
                          (fn [sections]
                            (if sub-section
                              (conj (or sections []) sub-section)
                              sections)))) {})))


(mount/defstate counts
  :start (->> questions
              (reduce
                (fn [a question]
                  (-> a
                      (update nil (fnil inc 0))
                      (update (-> question :section :number) (fnil inc 0))
                      (update [(-> question :section :number) (-> question :sub-section :number)] (fnil inc 0)))) {})))


(defonce topics (atom {}))


(defn parse-int
  [x]
  (try
    (Integer/parseInt x)
    (catch Exception _ nil)))


(def digits
  ["0⃣" "1⃣" "2⃣" "3⃣" "4⃣"
   "5⃣" "6⃣" "7⃣" "8⃣" "9⃣"])


(defn hash-tag [question]
  (->> [(-> question :section :number)
        (-> question :sub-section :number)
        (-> question :number)]
       (filter identity)
       (str/join "_")
       (str "#савол_")))


(defn open-question
  [question chat]
  (let [text     (str (hash-tag question) " "
                      (:text question) "\n\n"
                      (->> (:options question)
                           (sort-by :number)
                           (map (fn [{:keys [number text]}]
                                  (format "%s %s" (nth digits number) text)))
                           (str/join "\n")))
        keyboard {:inline_keyboard
                  [(->> (:options question)
                        (sort-by :number)
                        (map (fn [{:keys [number]}]
                               {:text          number
                                :callback_data (str (-> question :section :number) "."
                                                    (-> question :sub-section :number) "."
                                                    (-> question :number) "."
                                                    number)})))]}]
    (let [{{message_id :message_id} :result}
          (if (:image question)
            (:body
              (http/post
                (str "https://api.telegram.org/bot" (:bot-token env) "/sendPhoto")
                {:as        :json
                 :multipart [{:part-name "chat_id" :encoding "UTF-8" :content (str chat)}
                             {:part-name "photo" :encoding "UTF-8" :content (io/input-stream (io/resource (str "public/img/" (:image question)))) :name "photo.png"}
                             {:part-name "caption"
                              :encoding  "UTF-8"
                              :content   text}
                             {:part-name "reply_markup"
                              :encoding  "UTF-8"
                              :content   (generate-string keyboard)}]}))
            (t/send-text
              (:bot-token env)
              chat
              {:reply_markup keyboard}
              text))]
      (-> {:message_id  message_id
           :user_id     chat
           :section     (-> question :section :number)
           :sub_section (-> question :sub-section :number)
           :question    (-> question :number)
           :created_at  (util/now)}
          (query/insert-answer)
          (db/query)))))


(defn close-question
  [question message chat]
  (let [text (str (hash-tag question) " "
                  (:text question) "\n\n✅ "
                  (->> (:options question)
                       (filter #(= (:number %) (:correct question)))
                       (first)
                       (:text)))]
    (if (:image question)
      (:body
        (http/post
          (str "https://api.telegram.org/bot" (:bot-token env) "/editMessageCaption")
          {:content-type :json
           :as           :json
           :form-params  {:chat_id    chat
                          :message_id message
                          :caption    text}}))
      (t/edit-text
        (:bot-token env)
        chat
        message
        text))
    (-> {:message_id message
         :user_id    chat
         :guessed_at (util/now)}
        (query/update-answer)
        (db/query))))


(def standard-keyboard
  {:resize_keyboard true
   :keyboard        [["Кейинги савол" "Мазвулар"]]})


(defn next-question [chat question]
  (let [topic (get @topics chat)]
    (when (some? topic)
      (let [[section sub-section] topic
            questions (cond->> questions
                               (some? section) (filter #(= section (-> % :section :number)))
                               (some? sub-section) (filter #(= sub-section (-> % :sub-section :number))))]
        (or (when question
              (->> questions
                   (sort-by (juxt
                              #(-> % :section :number (or 0))
                              #(-> % :sub-section :number (or 0))
                              #(-> % :number (or 0))))
                   (partition 2 1)
                   (some (fn [[p n]]
                           (when (= question p) n)))))
            (first questions))))))


(defhandler bot
  (fn [update]
    (let [from (or (-> update :message :from)
                   (-> update :callback_query :from))]
      (-> {:id         (:id from)
           :first_name (:first_name from)
           :last_name  (:last_name from)
           :username   (:username from)}
          (query/upsert-user)
          (db/query)))
    nil)
  (callback-fn
    (fn [{id                    :id
          {chat :id}            :from
          {message :message_id} :message
          data                  :data}]
      (when id
        (let [[_ section-number sub-section-number question-number option-number]
              (re-find #"^(\d+)\.(\d*)\.(\d+)\.(\d+)$" data)
              question (->> questions
                            (filter (fn [{:keys [number section sub-section]}]
                                      (and (= number (parse-int question-number))
                                           (= (:number section) (parse-int section-number))
                                           (= (:number sub-section) (parse-int sub-section-number)))))
                            (first))]
          (if (= (:correct question) (parse-int option-number))
            (do (close-question question message chat)
                (if-let [question (next-question chat question)]
                  (open-question question chat)))
            (do (t/answer-callback (:bot-token env) id "❌ Жавоб нотўғри")
                (-> (query/append-answer-try
                      message
                      chat
                      (parse-int option-number))
                    (db/query))))))))

  (message-fn
    (fn [{message :text {chat :id} :from}]
      (when message
        (cond
          (= "Кейинги савол" message)
          (if-let [question (next-question chat nil)]
            (open-question question chat)
            (t/send-text
              (:bot-token env)
              chat
              "Мавзу танлагмаган ⚠️"))
          (re-matches (re-pattern (str select-all-text " \\(\\d+\\)")) message)
          (do
            (swap! topics assoc chat [])
            (t/send-text
              (:bot-token env)
              chat
              {:parse_mode   "markdown"
               :reply_markup standard-keyboard}
              (format "Мавзу танланди: %s\nСаволлар сони: %d" select-all-text (count questions))))
          (= "Мазвулар" message)
          (t/send-text
            (:bot-token env)
            chat
            {:reply_markup
             {:resize_keyboard true
              :keyboard        (->> (keys sections)
                                    (sort-by :number)
                                    (map #(str (:number %) ". " (:title %) " (" (counts (:number %)) ")"))
                                    (map vector)
                                    (concat [[(str select-all-text " (" (count questions) ")")]]))}}
            "Мавзуни танланг:")

          (re-find #"^(\d+)\.(\d+)\. (.+) \(\d+\)$" message)
          (let [[_ section-number number title] (re-find #"^(\d+)\.(\d+)\. (.+) \(\d+\)$" message)
                section     (->> (keys sections)
                                 (filter #(= (:number %) (parse-int section-number)))
                                 (first))
                sub-section (->> {:number (parse-int number) :title title}
                                 (get (set (sections section))))]

            (swap! topics assoc chat [(:number section) (:number sub-section)])
            (t/send-text
              (:bot-token env)
              chat
              {:parse_mode   "markdown"
               :reply_markup standard-keyboard}
              (format "Мавзу танланди: *%d.%d. %s*\nСаволлар сони: %d"
                      (:number section)
                      (:number sub-section)
                      (:title sub-section)
                      (counts [(:number section) (:number sub-section)]))))

          (re-find #"^(\d+)\. (.+) \(\d+\)$" message)
          (let [[_ number text] (re-find #"^(\d+)\. (.+) \(\d+\)$" message)
                section {:number (parse-int number) :title text}]
            (cond
              (= select-all-text (:title section))
              (let [section (->> (keys sections)
                                 (filter #(= (:number section) (:number %)))
                                 (first))]
                (swap! topics assoc chat [(:number section)])
                (t/send-text
                  (:bot-token env)
                  chat
                  {:parse_mode   "markdown"
                   :reply_markup standard-keyboard}
                  (format "Мавзу танланди: *%d. %s*\nСаволлар сони: %d"
                          (:number section)
                          (:title section)
                          (counts (:number section)))))
              (sections section)
              (t/send-text
                (:bot-token env)
                chat
                {:reply_markup
                 {:resize_keyboard true
                  :keyboard        (->> (sections section)
                                        (sort-by :number)
                                        (map #(str (:number section) "." (:number %) ". " (:title %) " (" (counts [(:number section) (:number %)]) ")"))
                                        (map vector)
                                        (concat [[(str (:number section) ". " select-all-text " (" (counts (:number section)) ")")]]))}}
                "Мавзуни танланг:")
              (contains? sections section)
              (do
                (swap! topics assoc chat [(:number section)])
                (t/send-text
                  (:bot-token env)
                  chat
                  {:parse_mode   "markdown"
                   :reply_markup standard-keyboard}
                  (format "Мавзу танланди: *%d. %s*\nСаволлар сони: %d"
                          (:number section)
                          (:title section)
                          (counts (:number section)))))))))))

  (message-fn
    (fn [{{user :id} :from}]
      (t/send-text
        (:bot-token env)
        user
        {:reply_markup standard-keyboard}
        "Хуш келибсиз"))))
