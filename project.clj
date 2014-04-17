(defproject me.raynes/fs "1.4.5"
  :description "File system utilities for clojure"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/Raynes/fs"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.commons/commons-compress "1.4"]]
  :plugins [[lein-midje "3.0-alpha4"]
            [codox "0.6.7"]]
  :profiles {:dev {:dependencies [[midje "1.5-alpha8"]]}})
