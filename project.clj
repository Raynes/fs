(defproject fs "1.1.2"
  :description "File system utilities for clojure"
  :url "https://github.com/Raynes/fs"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.commons/commons-compress "1.3"]]
  :aliases {"test-all" ["with-profile" "dev,default:dev,1.2,default:dev,1.3,default" "test"]}
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}})
