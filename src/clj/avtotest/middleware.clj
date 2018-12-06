(ns avtotest.middleware
  (:require [muuntaja.core :as muuntaja]
            [avtotest.config :refer [env]]
            [clojure.tools.logging :as log]
            [avtotest.env :refer [defaults]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]])
  (:import [com.fasterxml.jackson.datatype.jdk8 Jdk8Module]))


(def muuntaja
  (muuntaja/create
    (-> muuntaja/default-options
        (assoc-in
          [:formats "application/json" :opts :modules]
          [(Jdk8Module.)]))))


(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        {:status 500
         :body   "We've dispatched a team of highly trained gnomes to take care of the problem."}))))


(defn wrap-formats [handler]
  (-> handler
      (wrap-params)
      (wrap-format muuntaja)))


(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-webjars
      (wrap-defaults
        (-> site-defaults
            (dissoc :session)
            (dissoc :security)))
      wrap-internal-error))
