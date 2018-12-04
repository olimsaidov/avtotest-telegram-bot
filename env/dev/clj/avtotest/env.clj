(ns avtotest.env
  (:require [selmer.parser :as parser]
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
