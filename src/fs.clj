(ns ^{:doc "File system utilities in Clojure"
      :author "Miki Tebeka <miki.tebeka@gmail.com>"}
  fs
  (:require [clojure.zip :as zip])
  (:import java.io.File
           java.io.FileInputStream
           java.io.FileOutputStream
           java.io.FilenameFilter))

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

; FIXME: Write this
;(defn rmtree [root] ...)

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

(defn copy [from to]
  (let [from (File. from)
        to (File. to)]
    (when (not (.exists to)) (.createNewFile to))
    (with-open [to-channel (.getChannel (FileOutputStream. to))
                from-channel (.getChannel (FileInputStream. from))]
      (.transferFrom to-channel from-channel 0 (.size from-channel)))))

; FIXME: Write this
; (defn copytree [from to] ...

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

; Taken from https://github.com/jkk/clj-glob. (thanks Justin!)
(defn- glob->regex
  "Takes a glob-format string and returns a regex."
  [s]
  (loop [stream s
         re ""
         curly-depth 0]
    (let [[c j] stream]
        (cond
         (nil? c) (re-pattern (str (if (= \. (first s)) "" "(?=[^\\.])") re))
         (= c \\) (recur (nnext stream) (str re c c) curly-depth)
         (= c \/) (recur (next stream) (str re (if (= \. j) c "/(?=[^\\.])"))
                         curly-depth)
         (= c \*) (recur (next stream) (str re "[^/]*") curly-depth)
         (= c \?) (recur (next stream) (str re "[^/]") curly-depth)
         (= c \{) (recur (next stream) (str re \() (inc curly-depth))
         (= c \}) (recur (next stream) (str re \)) (dec curly-depth))
         (and (= c \,) (< 0 curly-depth)) (recur (next stream) (str re \|)
                                                 curly-depth)
         (#{\. \( \) \| \+ \^ \$ \@ \%} c) (recur (next stream) (str re \\ c)
                                                  curly-depth)
         :else (recur (next stream) (str re c) curly-depth)))))

(defn glob [pattern]
  "Returns files matching glob pattern."
  (let [parts (split pattern)
        root (if (= (count parts) 1) "." (apply join (butlast parts)))
        regex (glob->regex (last parts))]
    (map #(.getPath %) (seq (.listFiles (File. root)
                                        (reify FilenameFilter
                                          (accept [_ _ filename]
                                            (if (re-find regex filename)
                                              true false))))))))
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

(defn touch [path & time]
  "Set file modification time (default to now)"
  (let [file (File. path)]
    (.setLastModified file (if time time (System/currentTimeMillis)))))
