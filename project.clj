(defproject fs "1.3.2"
  :description "File system utilities for clojure"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/Raynes/fs"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.commons/commons-compress "1.3"]]
  :aliases {"testall" ["with-profile" "dev,default:dev,1.2,default:dev,1.3,default" "test"]}
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}})
