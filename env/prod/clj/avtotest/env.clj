(ns avtotest.env
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[avtotest started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[avtotest has shut down successfully]=-"))
   :middleware identity})


(mount/defstate poll
  :start :no-op)
