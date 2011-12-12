(ns fs.core-test
  (:use fs.core
        midje.sweet)
  (:require [clojure.java.io :as io])
  (:import java.io.File))

(def system-tempdir (System/getProperty "java.io.tmpdir"))

(defn create-walk-dir []
  (let [root (temp-dir)]
    (mkdir (file root "a"))
    (mkdir (file root "b"))
    (spit (file root "1") "1")
    (spit (file root "a" "2") "1")
    (spit (file root "b" "3") "1")
    root))

(fact "Makes paths absolute."
  (file ".") => @cwd
  (file "foo") => (io/file @cwd "foo"))

(fact (list-dir ".") => (has every? string?))

;; Want to change these files to be tempfiles at some point.
(against-background
 [(before :contents
          (let [f (io/file "test/fs/testfiles/bar")]
            (.setExecutable f false)
            (.setReadable f false)
            (.setWritable f false)))
  (after :contents
         (let [f (io/file "test/fs/testfiles/bar")]
           (.setExecutable f true)
           (.setReadable f true)
           (.setWritable f true)))]
 (fact
   (executable? "test/fs/testfiles/foo") => true
   (executable? "test/fs/testfiles/bar") => false)

 (fact
   (readable? "test/fs/testfiles/foo") => true
   (readable? "test/fs/testfiles/bar") => false)

 (fact
   (writeable? "test/fs/testfiles/foo") => true
   (writeable? "test/fs/testfiles/bar") => false))

(fact
  (file? "test/fs/testfiles/foo") => true
  (file? ".") => false)

(fact
  (exists? "test/fs/testfiles/foo") => true
  (exists? "ewjgnr4ig43j") => false)

(fact
  (let [f (io/file "test/fs/testfiles/baz")]
    (.createNewFile f)
    (delete f) =not=> (exists? f)))

(fact
  (directory? ".") => true
  (directory? "test/fs/testfiles/foo") => false)

(fact
  (file? ".") => false
  (file? "test/fs/testfiles/foo") => true)

(fact
  (let [tmp (temp-file)]
    (exists? tmp) => true
    (file? tmp) => true
    (delete tmp)))

(fact
  (let [tmp (temp-dir)]
    (exists? tmp) => true
    (directory? tmp) => true
    (delete tmp)))

(fact
  (absolute-path "foo") => (str (io/file @cwd "foo")))

(fact
  (normalized-path ".") => @cwd)

(fact
  (base-name "foo/bar") => "bar")

(fact
  (let [tmp (temp-file)]
    (> (mod-time tmp) 0) => true
    (delete tmp)))

(fact
  (let [f (temp-file)]
    (spit f "abc")
    (size f) => 3
    (delete f)))

(fact
  (let [root (create-walk-dir)
        result (delete-dir root)]
    (exists? root) => false
    root => result))

(fact
  (let [f (temp-file)]
    (delete f)
    (mkdir f)
    (directory? f) => true
    (delete-dir f)))

(fact
  (let [f (temp-file)
        sub (file f "a" "b")]
    (delete f)
    (mkdirs sub)
    (directory? sub) => true
    (delete-dir f)))

(fact
  (split (file "test/fs")) => (has-suffix ["test" "fs"]))

(fact
  (let [f (temp-file)
        new-f (str f "-new")]
    (rename f new-f)
    (exists? f) => false
    (exists? new-f) => true
    (delete new-f)))

(fact
  (let [root (create-walk-dir)]
    (walk vector root) => (contains [[root #{"b" "a"} #{"1"}]
                                     [(file root "a") #{} #{"2"}]
                                     [(file root "b") #{} #{"3"}]])
    (delete-dir root)))

(fact
  (let [from (temp-file)
        to (temp-file)
        data "What's up Doc?"]
    (delete to)
    (spit from data)
    (copy from to)
    (slurp from) => (slurp to)
    (delete from)
    (delete to)))

(fact
  (let [f (temp-file)
        t (mod-time f)]
    (Thread/sleep 1000)
    (touch f)
    (> (mod-time f) t) => true
    (let [t2 3000]
      (touch f t2)
      (mod-time f) => t2)
    (delete f)))

(fact
  (let [f (temp-file)]
    (chmod "+x" f)
    (executable? f) => true
    (when-not (re-find #"Windows" (System/getProperty "os.name"))
      (chmod "-x" f)
      (executable? f) => false)
    (delete f)))

(fact
  (let [from (create-walk-dir)
        to (temp-dir)
        path (copy-dir from to)
        dest (file to (base-name from))]
    path => dest
    (walk vector to) => (contains [[to #{(base-name from)} #{}]
                                   [dest #{"b" "a"} #{"1"}]
                                   [(file dest "a") #{} #{"2"}]
                                   [(file dest "b") #{} #{"3"}]])
    (delete-dir from)
    (delete-dir to)))

(when (System/getenv "HOME")
  (fact
    (home) => (System/getenv "HOME")))

(tabular
  (fact (split-ext ?file) => ?ext)
  
    ?file            ?ext
    "fs.clj"        ["fs" "clj"]
    "fs."           ["fs" ""]
    "fs.clj.bak"    ["fs.clj" "bak"]
    "/path/to/fs"   ["fs" nil]
    ""              ["fs" nil])

(tabular
  (fact (extension ?file) => ?ext)
  
    ?file            ?ext
    "fs.clj"        "clj"
    "fs."           ""
    "fs.clj.bak"    "bak"
    "/path/to/fs"   nil
    ""              nil)

(tabular
  (fact (name ?file) => ?ext)
  
    ?file            ?ext
    "fs.clj"        "fs"
    "fs."           "fs"
    "fs.clj.bak"    "fs.clj"
    "/path/to/fs"   "fs"
    ""              "fs")

(fact
  (let [old @cwd]
    (chdir "test")
    @cwd => (io/file old "test")))

(against-background
 [(before :contents (chdir "fs/testfiles"))]
 
 (fact
   (unzip "ggg.zip" "zggg")
   (exists? "zggg/ggg") => true
   (exists? "zggg/hhh/jjj") => true
   (delete-dir "zggg"))
 
 (fact
   (untar "ggg.tar" "zggg")
   (exists? "zggg/ggg") => true
   (exists? "zggg/hhh/jjj") => true
   (delete-dir "zggg"))

 (fact
   (gunzip "ggg.gz" "ggg")
   (exists? "ggg") => true
   (delete "ggg")))