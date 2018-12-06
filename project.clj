(defproject avtotest "0.1.0"

  :dependencies [[clojure.java-time "0.3.2"]
                 [com.fasterxml.jackson.core/jackson-core "2.9.7"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jdk8 "2.9.7"]
                 [compojure "1.6.1"]
                 [cprop "0.1.13"]
                 [luminus-immutant "0.2.4"]
                 [luminus/ring-ttl-session "0.3.2"]
                 [metosin/muuntaja "0.6.1"]
                 [metosin/ring-http-response "0.9.1"]
                 [mount "0.1.14"]
                 [nrepl "0.4.5"]
                 [morse "0.4.0"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.webjars/bootstrap "4.1.3"]
                 [org.webjars/font-awesome "5.5.0"]
                 [org.webjars/webjars-locator "0.34"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.5"]]

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
                       :dependencies   [[expound "0.7.1"]
                                        [prone "1.6.1"]]}})
