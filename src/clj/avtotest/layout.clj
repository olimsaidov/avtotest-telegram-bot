(ns avtotest.layout
  (:require [selmer.parser :as parser]
            [ring.util.http-response :refer [content-type ok]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))


(-> (clojure.java.io/resource "html")
    (parser/set-resource-path!))


(defn render
  "renders the HTML template located relative to resources/html"
  [template & [params]]
  (-> (parser/render-file template params)
      (ok)
      (content-type "text/html; charset=utf-8")))
