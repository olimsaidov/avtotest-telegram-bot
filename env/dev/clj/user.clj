(ns user
  (:require [avtotest.db]
            [mount.core :as mount]
            [conman.core :as conman]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [avtotest.config :refer [env]]
            [avtotest.core :refer [start-app]]
            [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'avtotest.core/repl-server))

(defn stop []
  (mount/stop-except #'avtotest.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn reset-db []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration [name]
  (migrations/create name (select-keys env [:database-url])))


