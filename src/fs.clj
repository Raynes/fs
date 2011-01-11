(ns ^{:doc "File system utilities in Clojure"
      :author "Miki Tebeka <miki.tebeka@gmail.com>"}
  fs
  (:require [clojure.zip :as zip])
  (:import java.io.File))

(def separator File/separator)

(defn listdir
  "List files under path."
  [path]
  (seq (.list (File. path))))

(defn executable?
  "Return true if path is executable."
  [path]
  (.canExecute (File. path)))

(defn readable?
  "Return true if path is readable."
  [path]
  (.canRead (File. path)))

(defn writeable?
  "Return true if path is writeable."
  [path]
  (.canWrite (File. path)))

(defn delete
  "Delete path."
  [path]
  (.delete (File. path)))

(defn exists?
  "Return true if path exists."
  [path]
  (.exists (File. path)))

(defn abspath
  "Return absolute path."
  [path]
  (.getAbsolutePath (File. path)))

(defn- strinfify [file]
  (.getCanonicalPath file))

(defn normpath
  "Return nomralized (canonical) path."
  [path]
  (strinfify (File. path)))

(defn basename
  "Return basename (file part) of path.\n\t(basename \"/a/b/c\") -> \"c\""
  [path]
  (.getName (File. path)))

(defn dirname
  "Return directory name of path.\n\t(dirname \"a/b/c\") -> \"/a/b\""
  [path]
  (.getParent (File. path)))

(defn directory?
  "Return true if path is a directory."
  [path]
  (.isDirectory (File. path)))

(defn file?
  "Return true if path is a file."
  [path]
  (.isFile (File. path)))

(defn mtime
  "Return file modification time."
  [path]
  (.lastModified (File. path)))

(defn size
  "Return size (in bytes) if file."
  [path]
  (.length (File. path)))

(defn mkdir
  "Create a directory."
  [path]
  (.mkdir (File. path)))

(defn mkdirs
  "Make directory tree."
  [path]
  (.mkdirs (File. path)))

(defn join
  "Join parts of path.\n\t(join [\"a\" \"b\"]) -> \"a/b\""
  [& parts]
  (apply str (interpose separator parts)))

(defn split
  "Split path to componenets.\n\t(split \"a/b/c\") -> (\"a\" \"b\" \"c\")"
  [path]
  (into [] (.split path separator)))

(defn rename
  "Rename old-path to new-path."
  [old-path new-path]
  (.renameTo (File. old-path) (File. new-path)))

(defn tempfile 
  "Create a temporary file."
  ([] (tempfile "-fs-" ""))
  ([prefix] (tempfile prefix ""))
  ([prefix suffix] (.getAbsolutePath (File/createTempFile prefix suffix)))
  ([prefix suffix directory] 
   (.getAbsolutePath (File/createTempFile prefix suffix (File. directory)))))

(defn tempdir
  "Create a temporary directory"
  ([] (let [dir (File/createTempFile "-fs-" "")
            path (.getAbsolutePath dir)]
        (.delete dir)
        (.mkdir dir)
        path))
  ([root]
   (let [dir (File/createTempFile "-fs-" "" (File. root))
         path (.getAbsolutePath dir)]
     (.delete dir)
     (.mkdir dir)
     path)))

(defn cwd
  "Return the current working directory."
  []
  (abspath "."))

; FIXME:
;(defn glob [pattern]

; walk helper functions
(defn- w-directory? [f]
  (.isDirectory f))
(defn- w-file? [f]
  (.isFile f))
(defn- w-children [f]
  (.listFiles f))
(defn- w-base [f]
  (.getName f))

; FIXME: I'm sure the Clojure gurus out there will make this a 1 liner :)
(defn walk [path func]
  "Walk over directory structure. Calls 'func' with [root dirs files]"
  (loop [loc (zip/zipper w-directory? w-children nil (File. path))]
    (when (not (zip/end? loc))
      (let [file (zip/node loc)]
        (if (w-file? file)
          (recur (zip/next loc))
          (let [kids (w-children file)
                dirs (set (map w-base (filter w-directory? kids)))
                files (set (map w-base (filter w-file? kids)))]
            (func (strinfify file) dirs files)
            (recur (zip/next loc))))))))
