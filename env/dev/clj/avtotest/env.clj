(ns avtotest.env
  (:require [selmer.parser :as parser]
            [morse.polling :as poll]
            [mount.core :as mount]
            [avtotest.config :refer [env]]
            [avtotest.bot :refer [bot]]
            [clojure.tools.logging :as log]
            [avtotest.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[avtotest started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[avtotest has shut down successfully]=-"))
   :middleware wrap-dev})


(mount/defstate poll
  :start (poll/start (:bot-token env) #'bot)
  :stop (poll/stop poll))
