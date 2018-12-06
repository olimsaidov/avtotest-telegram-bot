(ns avtotest.core
  (:require [mount.core :as mount]
            [avtotest.nrepl :as nrepl]
            [avtotest.handler :as handler]
            [luminus.http-server :as http]
            [avtotest.config :refer [env]]
            [clojure.tools.logging :as log])
  (:gen-class))


(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
    (-> env
        (assoc  :handler #'handler/app)
        (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
        (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))


(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))


(defn start-app []
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))


(defn -main [& _]
  (start-app))
