(ns avtotest.config
  (:require [cprop.source :as source]
            [cprop.core :refer [load-config]]
            [mount.core :refer [args defstate]]))


(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env)]))
