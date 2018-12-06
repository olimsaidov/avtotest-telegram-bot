(ns avtotest.handler
  (:require [mount.core :as mount]
            [compojure.route :as route]
            [avtotest.bot :refer [bot]]
            [avtotest.layout :as layout]
            [clojure.tools.logging :as log]
            [avtotest.env :refer [defaults]]
            [avtotest.middleware :as middleware]
            [compojure.core :refer [routes wrap-routes defroutes GET POST]]))


(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))



(defroutes home-routes
  (GET "/" []
    (layout/render "base.html"))
  (POST "/telegram" request
    (when (seq (:params request))
      (try
        (-> request :params bot)
        (catch Throwable t
          (log/error t (.getMessage t)))))
    {:status 200}))


(mount/defstate app
  :start
  (middleware/wrap-base
    (routes
      (-> #'home-routes
          (wrap-routes middleware/wrap-formats))
      (route/not-found "Page not found"))))

