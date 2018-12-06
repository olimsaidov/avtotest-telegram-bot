(ns avtotest.util)


(defn now []
  (java.util.Date.))


(defn random-uuid []
  (java.util.UUID/randomUUID))
