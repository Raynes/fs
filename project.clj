(defproject me.raynes/fs "1.4.6"
  :description "File system utilities for clojure"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/Raynes/fs"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.apache.commons/commons-compress "1.8"]]
  :plugins [[lein-midje "3.1.3"]
            [codox "0.8.10"]]
  :codox {:src-dir-uri "https://github.com/Raynes/fs/blob/master/"
          :src-linenum-anchor-prefix "L"
          :defaults {:doc/format :markdown}}
  :deploy-repositories {"releases" :clojars}
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}})
