(ns me.raynes.core-test
  (:refer-clojure :exclude [name parents])
  (:require [me.raynes.fs :refer :all]
            [me.raynes.fs.compression :refer :all]
            [midje.sweet :refer :all]
            [clojure.java.io :as io]) 
  (:import java.io.File))

(def system-tempdir (System/getProperty "java.io.tmpdir"))

(defn create-walk-dir []
  (let [root (temp-dir "fs-")]
    (mkdir (file root "a"))
    (mkdir (file root "b"))
    (spit (file root "1") "1")
    (spit (file root "a" "2") "1")
    (spit (file root "b" "3") "1")
    root))

(fact "Makes paths absolute."
  (file ".") => *cwd*
  (file "foo") => (io/file *cwd* "foo"))

(fact "Expands path to current user."
  (let [user (System/getProperty "user.home")]
    (expand-home "~") => (file user)
    (expand-home "~/foo") => (file user "foo")))

(fact "Expands to given user."
  (let [user (System/getProperty "user.home")
        name (System/getProperty "user.name")]
    (expand-home (str "~" name)) => (file user)
    (expand-home (format "~%s/foo" name)) => (file user "foo")))

(fact (list-dir ".") => (has every? string?))

;; Want to change these files to be tempfiles at some point.
(against-background
 [(around :contents (let [f (io/file "test/me/raynes/testfiles/bar")]
                      (.setExecutable f false)
                      (.setReadable f false)
                      (.setWritable f false)
                      ?form
                      (.setExecutable f true)
                      (.setReadable f true)
                      (.setWritable f true)))]
 (fact
   (executable? "test/me/raynes/testfiles/foo") => true
   (executable? "test/me/raynes/testfiles/bar") => false)

 (fact
   (readable? "test/me/raynes/testfiles/foo") => true
   (readable? "test/me/raynes/testfiles/bar") => false)

 (fact
   (writeable? "test/me/raynes/testfiles/foo") => true
   (writeable? "test/me/raynes/testfiles/bar") => false))

(fact
  (file? "test/me/raynes/testfiles/foo") => true
  (file? ".") => false)

(fact
  (exists? "test/me/raynes/testfiles/foo") => true
  (exists? "ewjgnr4ig43j") => false)

(fact
  (let [f (io/file "test/me/raynes/testfiles/baz")]
    (.createNewFile f)
    (delete f)
    (exists? f) => false))

(fact
  (directory? ".") => true
  (directory? "test/me/raynes/testfiles/foo") => false)

(fact
  (file? ".") => false
  (file? "test/me/raynes/testfiles/foo") => true)

(fact
  (let [tmp (temp-file "fs-")]
    (exists? tmp) => true
    (file? tmp) => true
    (delete tmp)))

(fact
  (let [tmp (temp-dir "fs-")]
    (exists? tmp) => true
    (directory? tmp) => true
    (delete tmp)))

(fact
  (absolute-path "foo") => (str (io/file *cwd* "foo")))

(fact
  (normalized-path ".") => *cwd*)

(fact
  (base-name "foo/bar") => "bar"
  (base-name "foo/bar.txt" true) => "bar"
  (base-name "bar.txt" ".txt") => "bar"
  (base-name "foo/bar.txt" ".png") => "bar.txt")

(fact
  (let [tmp (temp-file "fs-")]
    (> (mod-time tmp) 0) => true
    (delete tmp)))

(fact
  (let [f (temp-file "fs-")]
    (spit f "abc")
    (size f) => 3
    (delete f)))

(fact
  (let [root (create-walk-dir)
        result (delete-dir root)]
    (exists? root) => false))

(fact
  (let [f (temp-file "fs-")]
    (delete f)
    (mkdir f)
    (directory? f) => true
    (delete-dir f)))

(fact
  (let [f (temp-file "fs-")
        sub (file f "a" "b")]
    (delete f)
    (mkdirs sub)
    (directory? sub) => true
    (delete-dir f)))

(fact
  (split (file "test/fs")) => (has-suffix ["test" "fs"]))

(when unix-root
  (fact
   (split (file "/tmp/foo/bar.txt")) => '("/" "tmp" "foo" "bar.txt")
   (split (file "/")) => '("/")
   (split "/") => '("/")
   (split "") => '("")))

(fact
  (let [f (temp-file "fs-")
        new-f (str f "-new")]
    (rename f new-f)
    (exists? f) => false
    (exists? new-f) => true
    (delete new-f)))

(fact
  (let [root (create-walk-dir)]
    (walk vector root) => (contains [[root #{"b" "a"} #{"1"}]
                                     [(file root "a") #{} #{"2"}]
                                     [(file root "b") #{} #{"3"}]] 
                                    :in-any-order)
    (delete-dir root)))

(fact
  (let [from (temp-file "fs-")
        to (temp-file "fs-")
        data "What's up Doc?"]
    (delete to)
    (spit from data)
    (copy from to)
    (slurp from) => (slurp to)
    (delete from)
    (delete to)))

(fact
  (let [f (temp-file "fs-")
        t (mod-time f)]
    (Thread/sleep 1000)
    (touch f)
    (> (mod-time f) t) => true
    (let [t2 3000]
      (touch f t2)
      (mod-time f) => t2)
    (delete f)))

(fact
  (let [f (temp-file "fs-")]
    (chmod "+x" f)
    (executable? f) => true
    (when-not (re-find #"Windows" (System/getProperty "os.name"))
      (chmod "-x" f)
      (executable? f) => false)
    (delete f)))

(fact
  (let [from (create-walk-dir)
        to (temp-dir "fs-")
        path (copy-dir from to)
        dest (file to (base-name from))]
    path => dest
    (walk vector to) => (contains [[to #{(base-name from)} #{}]
                                   [dest #{"b" "a"} #{"1"}]
                                   [(file dest "a") #{} #{"2"}]
                                   [(file dest "b") #{} #{"3"}]]
                                  :in-any-order)
    (delete-dir from)
    (delete-dir to)))

(when (System/getenv "HOME")
  (fact
   (let [env-home (io/file (System/getenv "HOME"))]
     (home) => env-home
     (home "") => env-home
     (home (System/getProperty "user.name")) => env-home)))

(tabular
  (fact (split-ext ?file) => ?ext)
  
    ?file            ?ext
    "fs.clj"        ["fs" ".clj"]
    "fs."           ["fs" "."]
    "fs.clj.bak"    ["fs.clj" ".bak"]
    "/path/to/fs"   ["fs" nil]
    ""              ["fs" nil]
    "~user/.bashrc" [".bashrc" nil])

(tabular
  (fact (extension ?file) => ?ext)
  
    ?file            ?ext
    "fs.clj"        ".clj"
    "fs."           "."
    "fs.clj.bak"    ".bak"
    "/path/to/fs"   nil
    ""              nil
    ".bashrc"       nil)

(tabular
  (fact (name ?file) => ?ext)
  
    ?file            ?ext
    "fs.clj"        "fs"
    "fs."           "fs"
    "fs.clj.bak"    "fs.clj"
    "/path/to/fs"   "fs"
    ""              "fs"
    ".bashrc"       ".bashrc")

(fact "Can change cwd with with-cwd."
  (let [old *cwd*]
    (with-cwd "foo"
      *cwd* => (io/file old "foo"))))

(fact "Can change cwd mutably with with-mutable-cwd"
  (let [old *cwd*]
    (with-mutable-cwd
      (chdir "foo")
      *cwd* => (io/file old "foo"))))

(with-cwd "test/me/raynes/testfiles"
  (fact
    (unzip "ggg.zip" "zggg")
    (exists? "zggg/ggg") => true
    (exists? "zggg/hhh/jjj") => true
    (delete-dir "zggg"))

  (fact (zip "fro.zip" ["bbb.txt" "bbb"])
        (exists? "fro.zip") => true
        (unzip "fro.zip" "fro")
        (exists? "fro/bbb.txt") => true
        (delete "fro.zip")
        (delete-dir "fro"))

  (fact "about zip round trip"
    (zip "round.zip" ["some.txt" "some text"])
    (unzip "round.zip" "round")
    (slurp (file "round/some.txt")) => "some text")

  (fact
    (untar "ggg.tar" "zggg")
    (exists? "zggg/ggg") => true
    (exists? "zggg/hhh/jjj") => true
    (delete-dir "zggg"))

  (fact
    (gunzip "ggg.gz" "ggg")
    (exists? "ggg") => true
    (delete "ggg"))

  (fact
    (bunzip2 "bbb.bz2" "bbb")
    (exists? "bbb") => true
    (delete "bbb"))

  (fact
    (unxz "xxx.xz" "xxx")
    (exists? "xxx") => true
    (delete "xxx")))

(fact
  (parents "/foo/bar/baz/") => (just [(file "/foo")
                                      (file "/foo/bar")
                                      (file "/")]
                                     :in-any-order)
  (parents "/") => nil)

(fact
  (child-of? "/foo/bar" "/foo/bar/baz") => truthy
  (child-of? "/foo/bar/baz" "/foo/bar") => falsey)

(fact
  (path-ns "foo/bar/baz_quux.clj") => 'foo.bar.baz-quux)

(fact
  (str (ns-path 'foo.bar.baz-quux)) => (has-suffix "foo/bar/baz_quux.clj"))

(fact
  (absolute? "/foo/bar") => true
  (absolute? "/foo/") => true
  (absolute? "foo/bar") => false
  (absolute? "foo/") => false)
