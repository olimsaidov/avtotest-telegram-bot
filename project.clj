(defproject avtotest "0.1.0"

  :dependencies [[nrepl "0.6.0"]
                 [morse "0.4.3"]
                 [cprop "0.1.14"]
                 [conman "0.8.3"]
                 [mount "0.1.16"]
                 [selmer "1.12.17"]
                 [honeysql "0.9.8"]
                 [compojure "1.6.1"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.7.1"]
                 [luminus-immutant "0.2.5"]
                 [metosin/muuntaja "0.6.4"]
                 [clojure.java-time "0.3.2"]
                 [luminus-migrations "0.6.5"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/clojure "1.10.1"]
                 [org.webjars/bootstrap "4.3.1"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [org.webjars/font-awesome "5.10.1"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.postgresql/postgresql "42.2.6"]
                 [metosin/ring-http-response "0.9.1"]
                 [org.webjars/webjars-locator "0.37"]
                 [com.fasterxml.jackson.core/jackson-core "2.10.0.pr2"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jdk8 "2.10.0.pr2"]]

  :min-lein-version "2.0.0"
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main ^:skip-aot avtotest.core

  :profiles {:uberjar {:omit-source    true
                       :aot            :all
                       :uberjar-name   "avtotest.jar"
                       :source-paths   ["env/prod/clj"]
                       :resource-paths ["env/prod/resources"]}

             :dev     {:jvm-opts       ["-Dconf=dev-config.edn"]
                       :source-paths   ["env/dev/clj"]
                       :resource-paths ["env/dev/resources"]
                       :repl-options   {:init-ns user}
                       :dependencies   [[expound "0.7.2"]
                                        [prone "2019-07-08"]]}})
