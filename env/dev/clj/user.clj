(ns user
  (:require [avtotest.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [avtotest.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'avtotest.core/repl-server))

(defn stop []
  (mount/stop-except #'avtotest.core/repl-server))

(defn restart []
  (stop)
  (start))


