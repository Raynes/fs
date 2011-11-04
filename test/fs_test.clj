(ns fs-test
  (:import java.io.File)
  (:use [fs] :reload-all)
  (:use [clojure.test]))

(deftest listdir-test
  (is (not (empty? (listdir ".")))))

(deftest executable?-test
  (is (executable? ".")))

(deftest readable?-test
  (is (readable? ".")))

(deftest writeable?-test
  (is (writeable? ".")))

(deftest delete-test
  (let [f (tempfile)]
    (delete f)
    (is (not (exists? f)))))

(deftest exists?-test
  (is (exists? ".")))

; FIXME: This test sucks
(deftest abspath-test
  (is (> (count (abspath ".")) 2)))

; FIXME: This test sucks
(deftest normpath-test 
  (is (> (count (normpath ".")) 2)))

(deftest basename-test
  (is (= (basename "/a/b/c") "c")))

(deftest dirname-test
  (is (= (dirname (join "a" "b" "c")) (join "a" "b"))))

(deftest directory?-test
  (is (directory? ".")))

(deftest file?-test
  (let [tmp (tempfile)]
    (is (file? tmp))
    (delete tmp)))

; FIXME: This test sucks
(deftest mtime-test
  (let [tmp (tempfile)]
    (is (> (mtime tmp) 0))
    (delete tmp)))

(deftest size-test
  (let [f (tempfile)]
    (spit f "abc")
    (is (= (size f) 3))
    (delete f)))

(deftest mkdir-test
  (let [f (tempfile)]
    (delete f)
    (mkdir f)
    (is (directory? f))
    (deltree f)))

(deftest mkdirs-test
  (let [f (tempfile)
        sub (join f "a" "b")]
    (delete f)
    (mkdirs sub)
    (is (directory? sub))
    (deltree f)))

(deftest join-test
  (is (= (join "a" "b" "c") (apply str (interpose *separator* "abc")))))

(deftest split-test
  (is (= (split (apply str (interpose *separator* "abc"))) '("a" "b" "c"))))

(deftest rename-test
  (let [f (tempfile)
        new-f (str f "-new")]
    (rename f new-f)
    (is (not (exists? f)))
    (is (exists? new-f))
    (delete new-f)))

; FIXME: Test all variations of tempfile
(deftest tempfile-test
  (let [tmp (tempfile)]
    (is (file? tmp))
    (delete tmp)))

; FIXME: Test all variations of tempdir
(deftest tempdir-test
  (let [tmp (tempdir)]
    (is (directory? tmp))
    (deltree tmp)))

; FIXME: This test sucks
(deftest cwd-test
  (is (> (count (cwd)) 3)))

(defn create-walk-dir []
  (let [root (tempdir)]
    (mkdir (join root "a"))
    (mkdir (join root "b"))
    (spit (join root "1") "1")
    (spit (join root "a" "2") "1")
    (spit (join root "b" "3") "1")
    root))

(def walk-atom (atom #{}))

(defn walk-fn [root dirs files]
  (swap! walk-atom conj [root dirs files]))

(deftest walk-test
 (let [root (create-walk-dir)]
   (walk root walk-fn)
   (let [result @walk-atom]
     (is (= result
            #{[root #{"b" "a"} #{"1"}]
              [(join root "a") #{} #{"2"}]
              [(join root "b") #{} #{"3"}]}))
     (deltree root))))

(deftest copy-test
  (let [from (tempfile)
        to (tempfile)
        data "What's up Doc?"]
    (delete to)
    (spit from data)
    (copy from to)
    (is (= (slurp from) (slurp to)))
    (delete from)
    (delete to)))

(deftest touch-test
    (let [f (tempfile)
          t (mtime f)]
      (Thread/sleep 1000)
      (touch f)
      (is (> (mtime f) t))
      (let [t1 3000]
        (touch f t1)
        (is (= (mtime f) t1)))
      (delete f)))

(deftest test-chmod
  (let [f (tempfile)]
    (chmod "-x" f)
    (is (not (executable? f)))
    (chmod "+x" f)
    (is (executable? f))
    (delete f)))

(deftest test-copy-tree
  (let [from (create-walk-dir)
        to (tempdir)]
    (swap! walk-atom (fn [_] #{}))
    (let [path (copy-tree from to)
          dest (join to (basename from))]
      (is (= path dest))
      (walk to walk-fn)
      (let [result @walk-atom]
        (is (= result
               #{[to #{(basename from)} #{}]
                 [dest #{"b" "a"} #{"1"}]
                 [(join dest "a") #{} #{"2"}]
                 [(join dest "b") #{} #{"3"}]})))
      (deltree from)
      (deltree to))))

(deftest test-deltree
  (let [root (create-walk-dir)
        result (deltree root)]
    (is (not (exists? root)))
    (is (= root result))))

(deftest test-home
  (is (= (home) (System/getenv "HOME"))))

(deftest text-extension
  (is (= (extension "fs.clj") ".clj"))
  (is (= (extension "fs.") "."))
  (is (= (extension "/path/to/fs") ""))
  (is (= (extension "fs.clj.bak") ".bak"))
  (is (= (extension "") "")))

(deftest test-chdir
  (let [path "/tmp"]
    (chdir path)
    (is (= *cwd* path))
    (is (= (cwd) path))))

(deftest test-with-cwd
  (with-cwd "/tmp"
    (is (= (cwd) "/tmp")))
  (is (= (cwd) *cwd*)))

(deftest test-file
  ; Test that we work with java.io.File objects as well
  (is (directory? (File. "/tmp"))))
