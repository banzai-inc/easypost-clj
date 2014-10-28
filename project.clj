(defproject easypost-clj "0.1.2"
  :description "Clojure library for Easypost's shipping API"
  :url "http://github.com/banzai-inc/easypost-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.0"]]
  :profiles {:dev {:dependencies [[environ "1.0.0"]]}})
