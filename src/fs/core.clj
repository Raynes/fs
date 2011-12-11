(ns fs
  "File system utilities in Clojure"
  (:require [clojure.zip :as zip]
            [clojure.java.io :as io])
  (:import java.io.File
           java.io.FilenameFilter))

; File separator
(def ^:dynamic *separator* File/separator)
; Extension separator
(def ^:dynamic *extension-separator* ".")
(declare abspath)
; Current working directory (you can't change directory in java)
(def ^:dynamic *cwd* (.getCanonicalPath (io/as-file ".")))

(defn- as-file [path]
  "The challenge is to work nicely with *cwd*"
  (cond
   (instance? File path) path
   (= path "") (io/as-file "")
   (= path ".") (io/as-file *cwd*)
   :else
   (let [ try (new File path) ]
         (if (.isAbsolute try)
           try
           (new File *cwd* path)))))

(defn listdir
  "List files under path."
  [path]
  (seq (.list (as-file path))))

(defn executable?
  "Return true if path is executable."
  [path]
  (.canExecute (as-file path)))

(defn readable?
  "Return true if path is readable."
  [path]
  (.canRead (as-file path)))

(defn writeable?
  "Return true if path is writeable."
  [path]
  (.canWrite (as-file path)))

(defn delete
  "Delete path. Returns path."
  [path]
  (.delete (as-file path))
  path)

(defn exists?
  "Return true if path exists."
  [path]
  (.exists (as-file path)))

(defn abspath
  "Return absolute path."
  [path]
  (.getAbsolutePath (as-file path)))

(defn- strinfify [file]
  (.getCanonicalPath file))

(defn normpath
  "Return nomralized (canonical) path."
  [path]
  (strinfify (as-file path)))

(defn basename
  "Return basename (file part) of path.\n\t(basename \"/a/b/c\") -> \"c\"."
  [path]
  (.getName (as-file path)))

(defn extension
  "Return the file extension.\n\t(extension \"fs.clj\") -> \".clj\"."
  [path]
  (let [base (basename path)
        i (.lastIndexOf base *extension-separator*)]
    (if (pos? i)
      (subs base i)
      "")))

(defn dirname
  "Return directory name of path.\n\t(dirname \"a/b/c\") -> \"/a/b\"."
  [path]
  (.getParent (io/as-file path)))

(defn directory?
  "Return true if path is a directory."
  [path]
  (.isDirectory (as-file path)))

(defn file?
  "Return true if path is a file."
  [path]
  (.isFile (as-file path)))

(defn mtime
  "Return file modification time."
  [path]
  (.lastModified (as-file path)))

(defn size
  "Return size (in bytes) if file."
  [path]
  (.length (as-file path)))

(defn mkdir
  "Create a directory. Returns path."
  [path]
  (.mkdir (as-file path))
  path)

(defn mkdirs
  "Make directory tree. Returns path."
  [path]
  (.mkdirs (as-file path))
  path)

(defn join
  "Join parts of path.\n\t(join [\"a\" \"b\"]) -> \"a/b\"."
  [& parts]
  (apply str (interpose *separator* parts)))

(defn split
  "Split path to componenets.\n\t(split \"a/b/c\") -> (\"a\" \"b\" \"c\")."
  [path]
  (into [] (.split path (str "\\Q" *separator* "\\E"))))

(defn rename
  "Rename old-path to new-path."
  [old-path new-path]
  (.renameTo (as-file old-path) (as-file new-path)))

(defn- ensure-file [path]
  (let [file (as-file path)]
    (when-not (.exists file) (.createNewFile file))
    file))

(defn- assert-exists [path]
  (when-not (exists? path)
    (throw (IllegalArgumentException. (str path " not found")))))

(defn copy
  "Copy a file from 'from' to 'to'. Return 'to'."
  [from to]
  (assert-exists from)
  (io/copy (as-file from) (as-file to))
  to)

; FIXME: Have prefix, suffix and directory keword arguements and add
; delete-on-exit
(defn tempfile 
  "Create a temporary file."
  ([] (tempfile "-fs-" ""))
  ([prefix] (tempfile prefix ""))
  ([prefix suffix] (.getAbsolutePath (File/createTempFile prefix suffix)))
  ([prefix suffix directory] 
   (.getAbsolutePath 
     (File/createTempFile prefix suffix (as-file directory)))))

(defn tempdir
  "Create a temporary directory."
  ([] (let [dir (File/createTempFile "-fs-" "")
            path (.getAbsolutePath dir)]
        (.delete dir)
        (.mkdir dir)
        path))
  ([root]
   (let [dir (File/createTempFile "-fs-" "" (as-file root))
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
         (nil? c) (re-pattern 
                    ; We add ^ and $ since we check only for file names
                    (str "^" (if (= \. (first s)) "" "(?=[^\\.])") re "$"))
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

(defn glob
  "Returns files matching glob pattern."
  [pattern]
  (let [parts (split pattern)
        root (if (= (count parts) 1) "." (apply join (butlast parts)))
        regex (glob->regex (last parts))]
    (map #(.getPath %) (seq (.listFiles (as-file root)
                                        (reify FilenameFilter
                                          (accept [_ _ filename]
                                            (boolean (re-find regex 
                                                              filename)))))))))

(defn- iterzip
  "Iterate over a zip, returns a sequence of the nodes with a nil suffix"
  [z]
  (when-not (zip/end? z)
    (cons (zip/node z) (lazy-seq (iterzip (zip/next z))))))

(defn- f-dir? [f]
  (when f (.isDirectory f)))

(defn- f-children [f]
  (.listFiles f))

(defn- f-base [f]
  (.getName f))

(defn- iterdir* [path]
  (let [root (as-file path)
        nodes (butlast (iterzip (zip/zipper f-dir? f-children nil root)))]
    (filter f-dir? nodes)))

(defn- walk-map-fn [root]
  (let [kids (f-children root)
        dirs (set (map f-base (filter f-dir? kids)))
        files (set (map f-base (filter (complement f-dir?) kids)))]
    [(strinfify root) dirs files]))

(defn iterdir
  "Return a sequence [root dirs files], starting from path"
  [path]
  (map walk-map-fn (iterdir* path)))

(defn walk
  "Walk over directory structure. Calls 'func' with [root dirs files]"
  [path func]
  (dorun (map #(apply func %) (iterdir path))))

(defn touch
  "Set file modification time (default to now). Returns path."
  [path & time]
  (let [file (ensure-file path)]
    (.setLastModified file (if time (first time) (System/currentTimeMillis)))
    path))

(defn chmod
  "Change file permissions. Returns path.

  'mode' can be any combination of \"r\" (readable) \"w\" (writable) and \"x\"
  (executable). It should be prefixed with \"+\" to set or \"-\" to unset. And
  optional prefix of \"u\" causes the permissions to be set for the owner only.
  
  Examples:
  (chmod \"+x\" \"/tmp/foo\") -> Sets executable for everyone
  (chmod \"u-wx\" \"/tmp/foo\") -> Unsets owner write and executable"
  [mode path]
  (assert-exists path)
  (let [[_ u op permissions] (re-find #"^(u?)([+-])([rwx]{1,3})$" mode)]
    (when (nil? op) (throw (IllegalArgumentException. "Bad mode")))
    (let [perm-set (set permissions)
          file (as-file path)
          flag (= op "+")
          user (not (empty? u))]
      (when (perm-set \r) (.setReadable file flag user))
      (when (perm-set \w) (.setWritable file flag user))
      (when (perm-set \x) (.setExecutable file flag user)))
    path))

(defn copy+
  "Copy src to dest, create directories if needed."
  [src dest]
  (mkdirs (dirname dest))
  (copy src dest))

(defn copy-tree
  "Copy a directory from 'from' to 'to'."
  [from to]
  (when (file? to) 
    (throw (IllegalArgumentException. (format "%s is a file" to))))
  (let [from (normpath from)
        to (normpath (if (exists? to) (join to (basename from)) to))
        trim-size (inc (count from))
        dest #(join to (subs % trim-size))]
    (mkdirs to)
    (walk from
      (fn [root dirs files]
        (dorun (map #(when-not (directory? %) (mkdirs (dest (join root %)))) 
                    dirs))
        (dorun (map #(copy+ (join root %) (dest (join root %))) files))))
    to))

(defn deltree
  "Delete a directory tree."
  [root]
  (when (directory? root)
    (dorun (map deltree (map #(join root %) (.list (as-file root))))))
  (delete root))

(defn home
  "User home directory"
  []
  (System/getProperty "user.home"))

(defn chdir
  "Change directrory.
  
  This only changes the value of *cwd* (you can't change directory in Java)."
  [path]
  (intern (find-ns 'fs) '*cwd* (abspath path))
  path)

; We do this trick with letfn due to some problems with macros and binding
(letfn [[bind-cwd [path f] (binding [*cwd* path] (f))]]
  (defmacro with-cwd
    "Temporary change directory."
    [path & body]
    `(~bind-cwd ~path (fn [] ~@body))))

