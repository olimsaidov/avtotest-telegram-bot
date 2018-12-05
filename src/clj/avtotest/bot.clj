(ns avtotest.bot
  (:require [morse.handlers :refer :all]
            [morse.polling :as poll]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [morse.api :as t]
            [avtotest.config :refer [env]]))


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


(defn parse-int
  [x]
  (Integer/parseInt x))


(defhandler bot
  (message-fn
    (fn [{message :text {user :id} :from}]
      (cond
        (= "Мазвулар" message)
        (t/send-text
          (:bot-token env)
          user
          {:reply_markup
           {:resize_keyboard true
            :keyboard        (->> (keys sections)
                                  (sort-by :number)
                                  (map #(str (:number %) ". " (:title %)))
                                  (map vector))}}
          "Мавзуни танланг:")

        (re-find #"(\d+)\.(\d+)\. (.+)" message)
        (let [[_ section-number number title] (re-find #"(\d+)\.(\d+)\. (.+)" message)
              section      (->> (keys sections)
                                (filter #(= (:number %) (parse-int section-number)))
                                (first))
              sub-section  (->> {:number (parse-int number) :title title}
                                (get (set (sections section))))]

          (t/send-text
            (:bot-token env)
            user
            {:parse_mode   "markdown"
             :reply_markup {:resize_keyboard true
                            :keyboard        [["Мазвулар"]]}}
            (format "Мавзу танланди: *%d.%d. %s*" (:number section) (:number sub-section) (:title sub-section))))
        (re-find #"(\d+)\. (.+)" message)
        (let [[_ number text] (re-find #"(\d+)\. (.+)" message)
              section {:number (parse-int number) :title text}]
          (cond
            (= select-all-text (:title section))
            (let [section (->> (keys sections)
                               (filter #(= (:number section) (:number %)))
                               (first))]
              (t/send-text
                (:bot-token env)
                user
                {:parse_mode "markdown"
                 :reply_markup {:resize_keyboard true
                                :keyboard        [["Мазвулар"]]}}
                (format "Мавзу танланди: *%d. %s*" (:number section) (:title section))))
            (sections section)
            (t/send-text
              (:bot-token env)
              user
              {:reply_markup
               {:resize_keyboard true
                :keyboard        (->> (sections section)
                                      (sort-by :number)
                                      (map #(str (:number section) "." (:number %) ". " (:title %)))
                                      (map vector)
                                      (concat [[(str (:number section) ". " select-all-text)]]))}}
              "Мавзуни танланг:")
            (contains? sections section)
            (t/send-text
              (:bot-token env)
              user
              {:parse_mode "markdown"
               :reply_markup {:resize_keyboard true
                              :keyboard        [["Мазвулар"]]}}
              (format "Мавзу танланди: *%d. %s*" (:number section) (:title section))))))))

  (message-fn
    (fn [{{user :id} :from}]
      (t/send-text
        (:bot-token env)
        user
        {:reply_markup {:resize_keyboard true
                        :keyboard        [["Мазвулар"]]}}
        "Хуш келибсиз"))))



(mount/defstate poll
  :start (poll/start (:bot-token env) #'bot)
  :stop (poll/stop poll))
