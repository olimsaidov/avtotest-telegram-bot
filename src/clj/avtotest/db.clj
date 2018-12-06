(ns avtotest.db
  (:require [java-time :as jt]
            [honeysql.core :as sql]
            [conman.core :as conman]
            [clojure.java.jdbc :as jdbc]
            [avtotest.config :refer [env]]
            [mount.core :refer [defstate]]
            [cheshire.core :refer [generate-string parse-string]]))


(defstate ^:dynamic *db*
  :start (conman/connect! {:jdbc-url (env :database-url)})
  :stop (conman/disconnect! *db*))


(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (.toLocalTime v))
  java.sql.Array
  (result-set-read-column [v _ _] (vec (.getArray v)))
  org.postgresql.util.PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (parse-string value true)
        "jsonb" (parse-string value true)
        "citext" (str value)
        value))))


(defn to-pg-json [value]
      (doto (org.postgresql.util.PGobject.)
        (.setType "jsonb")
        (.setValue (generate-string value))))


(extend-protocol jdbc/ISQLParameter
  clojure.lang.IPersistentVector
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (to-pg-json v))))))


(extend-protocol jdbc/ISQLValue
  java.util.Date
  (sql-value [v]
    (java.sql.Timestamp. (.getTime v)))
  java.time.LocalTime
  (sql-value [v]
    (jt/sql-time v))
  java.time.LocalDate
  (sql-value [v]
    (jt/sql-date v))
  java.time.LocalDateTime
  (sql-value [v]
    (jt/sql-timestamp v))
  java.time.ZonedDateTime
  (sql-value [v]
    (jt/sql-timestamp v))
  clojure.lang.IDeref
  (sql-value [value] (jdbc/sql-value @value))
  clojure.lang.IPersistentMap
  (sql-value [value] (to-pg-json value))
  clojure.lang.IPersistentVector
  (sql-value [value] (to-pg-json value)))


(defn query
  ([sql-map]
   (query sql-map nil))
  ([sql-map row-fn]
   (jdbc/query
     *db*
     (sql/format sql-map)
     (when row-fn {:row-fn row-fn}))))
