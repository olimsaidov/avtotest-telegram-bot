(ns avtotest.routes.home
  (:require [avtotest.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [clojure.tools.logging :as log]
            [avtotest.bot :refer [bot]]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))
  (POST "/telegram" request
    (when (seq (:params request))
      (try
        (-> request :params bot)
        (catch Exception e
          (log/error e))))
    {:status 200}))
