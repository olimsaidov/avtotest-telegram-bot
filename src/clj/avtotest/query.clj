(ns avtotest.query
  (:refer-clojure :exclude [update select format partition-by group-by])
  (:require [honeysql.core :as sql]
            [honeysql.format :refer :all]
            [honeysql.helpers :refer :all]
            [honeysql-postgres.format :refer :all]
            [honeysql-postgres.helpers :refer :all]
            [clojure.string :as str])
  (:import clojure.lang.IDeref))


(defn raw-value
  [value]
  (reify IDeref
    (deref [_] value)))


(defn do-update-set* [sql-map columns]
  (apply do-update-set sql-map columns))


(defn upsert-user
  [user]
  (-> (insert-into :users)
      (values [user])
      (on-conflict :id)
      (do-update-set* (keys (dissoc user :id)))
      (returning :*)))


(defn insert-answer
  [answer]
  (-> (insert-into :answer)
      (returning :*)
      (values [answer])))


(defn append-answer-try
  [message_id user_id option]
  (-> (update :answer)
      (sset {:tries (sql/raw (clojure.core/format "coalesce(tries, '[]'::jsonb) || '[%d]'::jsonb" option))})
      (where [:and
              [:= :message_id message_id]
              [:= :user_id user_id]])
      (returning :*)))


(defn update-answer
  [answer]
  (-> (update :answer)
      (sset (dissoc answer :message_id :user_id))
      (where [:and
              [:= :message_id (:message_id answer)]
              [:= :user_id (:user_id answer)]])
      (returning :*)))
