(ns me.raynes.fs
  "File system utilities in Clojure"
  (:refer-clojure :exclude [name parents])
  (:require [clojure.zip :as zip]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.java.shell :as sh])
  (:import [java.io File FilenameFilter]
           [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]))

;; Once you've started a JVM, that JVM's working directory is set in stone
;; and cannot be changed. This library will provide a way to simulate a
;; working directory change. `cwd` is considered to be the current working
;; directory for functions in this library. Unfortunately, this will only
;; apply to functions inside this library since we can't change the JVM's
;; actual working directory.
(def ^{:doc "Current working directory. This cannot be changed in the JVM.
             Changing this will only change the working directory for functions
             in this library."
       :dynamic true}
  *cwd* (.getCanonicalFile (io/file ".")))

(let [homedir (io/file (System/getProperty "user.home"))
      usersdir (.getParent homedir)]
  (defn home
    "With no arguments, returns the current value of the user.home system
     property. If a user is passed, returns that user's home directory. It
     is naively assumed to be a directory with the same name as the user
     located relative to the parent of the current value of user.home."
    ([] homedir)
    ([user] (if (empty? user) homedir (io/file usersdir user)))))

(defn expand-home
  "If path begins with a tilde (~), expand the tilde to the value
   of the user.home system property. If the path begins with a tilde
   immediately followed by some characters, they are assumed to be a
   username. This is expanded to the path to that user's home directory.
   This is (naively) assumed to be a directory with the same name as the
   user relative to the parent of the current value of user.home."
  [path]
  (let [path (str path)]
    (if (.startsWith path "~")
      (let [sep (.indexOf path File/separator)]
        (if (neg? sep)
          (home (subs path 1))
          (io/file (home (subs path 1 sep)) (subs path (inc sep)))))
      path)))

;; Library functions will call this function on paths/files so that
;; we get the cwd effect on them.
(defn ^File file
  "If path is a period, replaces it with cwd and creates a new File object
   out of it and paths. Or, if the resulting File object does not constitute
   an absolute path, makes it absolutely by creating a new File object out of
   the paths and cwd."
  [path & paths]
  (when-let [path (apply
                   io/file (if (= path ".")
                             *cwd*
                             path)
                   paths)]
    (if (.isAbsolute ^File path)
      path
      (io/file *cwd* path))))

(extend-protocol clojure.java.io/Coercions
  Path
  (as-file [this] (.toFile this))
  (as-url [this] (. this (toFile) (toUrl))))

(defn list-dir
  "List files and directories under path."
  [path]
  (seq (.list (file path))))

(defn absolute?
  "Return true if path is absolute."
  [path]
  (.isAbsolute (io/file path)))

(defn executable?
  "Return true if path is executable."
  [path]
  (.canExecute (file path)))

(defn readable?
  "Return true if path is readable."
  [path]
  (.canRead (file path)))

(defn writeable?
  "Return true if path is writeable."
  [path]
  (.canWrite (file path)))

(defn delete
  "Delete path."
  [path]
  (.delete (file path)))

(defn exists?
  "Return true if path exists."
  [path]
  (.exists (file path)))

(defn absolute-path
  "Return absolute path."
  [path]
  (.getAbsolutePath (file path)))

(defn normalized-path
  "Return normalized (canonical) path."
  [path]
  (.getCanonicalFile (file path)))

(defn ^String base-name
  "Return the base name (final segment/file part) of a path.

   If optional `trim-ext` is a string and the path ends with that string,
   it is trimmed.

   If `trim-ext` is true, any extension is trimmed."
  ([path] (.getName (file path)))
  ([path trim-ext]
     (let [base (.getName (file path))]
       (cond (string? trim-ext) (if (.endsWith base trim-ext)
                                  (subs base 0 (- (count base) (count trim-ext)))
                                  base)
             trim-ext (let [dot (.lastIndexOf base ".")]
                        (if (pos? dot) (subs base 0 dot) base))
             :else base))))

(defn directory?
  "Return true if path is a directory."
  [path]
  (.isDirectory (file path)))

(defn file?
  "Return true if path is a file."
  [path]
  (.isFile (file path)))

(defn ^Boolean hidden?
  "Return true if path is hidden."
  [path]
  (.isHidden (file path)))

(defn- ^Path as-path
  "Convert path to a java.nio.file.Path."
  [path]
  (.toPath (file path)))

(defn ^Boolean link?
  "Return true if path is a link."
  [path]
  (Files/isSymbolicLink (as-path path)))

(defn ^File link
  "Create a \"hard\" link from path to target."
  [path target]
  (file (Files/createLink (as-path path) (as-path target))))

(defn ^File sym-link
  "Create a \"soft\" link from path to target."
  [path target]
  (file (Files/createSymbolicLink
         (as-path path)
         (as-path target)
         (make-array FileAttribute 0))))

(defn split-ext
  "Returns a vector of [name extension]."
  [path]
  (let [base (base-name path)
        i (.lastIndexOf base ".")]
    (if (pos? i)
      [(subs base 0 i) (subs base i)]
      [base nil])))

(defn extension
  "Return the extension part of a file."
  [path] (last (split-ext path)))

(defn name
  "Return the name part of a file."
  [path] (first (split-ext path)))

(defn parent
  "Return the parent path."
  [path]
  (.getParentFile (file path)))

(defn mod-time
  "Return file modification time."
  [path]
  (.lastModified (file path)))

(defn size
  "Return size (in bytes) of file."
  [path]
  (.length (file path)))

(defn mkdir
  "Create a directory."
  [path]
  (.mkdir (file path)))

(defn mkdirs
  "Make directory tree."
  [path]
  (.mkdirs (file path)))

(def ^{:doc "The root of a unix system is /, nil on Windows"}
  unix-root (when (= File/separator "/") File/separator))

(defn split
  "Split path to components."
  [path]
  (let [pathstr (str path)
        jregx (str "\\Q" File/separator "\\E")]
    (cond (= pathstr unix-root) (list unix-root)
          (and unix-root (.startsWith pathstr unix-root))
          ;; unix absolute path
            (cons unix-root (seq (.split (subs pathstr 1) jregx)))
          :else (seq (.split pathstr jregx)))))

(defn rename
  "Rename old-path to new-path. Only works on files."
  [old-path new-path]
  (.renameTo (file old-path) (file new-path)))

(defn create
  "Create a new file."
  [^File f]
  (.createNewFile f))

(defn- assert-exists [path]
  (when-not (exists? path)
    (throw (IllegalArgumentException. (str path " not found")))))

(defn copy
  "Copy a file from 'from' to 'to'. Return 'to'."
  [from to]
  (assert-exists from)
  (io/copy (file from) (file to))
  to)

(defn tmpdir
  "The temporary file directory looked up via the java.io.tmpdir
   system property. Does not create a temporary directory."
  []
  (System/getProperty "java.io.tmpdir"))

(defn temp-name
  "Create a temporary file name like what is created for temp-file
   and temp-dir."
  ([prefix] (temp-name prefix ""))
  ([prefix suffix]
     (format "%s%s-%s%s" prefix (System/currentTimeMillis)
             (long (rand 0x100000000)) suffix)))

(defn- temp-create
  "Create a temporary file or dir, trying n times before giving up."
  ([prefix suffix tries f]
     (loop [tries (range tries)]
       (let [tmp (file (tmpdir) (temp-name prefix suffix))]
         (if (and (seq tries) (f tmp))
           tmp
           (recur (rest tries)))))))

(defn temp-file
  "Create a temporary file. Returns nil if file could not be created
   even after n tries (default 10)."
  ([prefix]              (temp-file prefix "" 10))
  ([prefix suffix]       (temp-file prefix suffix 10))
  ([prefix suffix tries] (temp-create prefix suffix tries create)))

(defn temp-dir
  "Create a temporary directory. Returns nil if dir could not be created
   even after n tries (default 10)."
  ([prefix]              (temp-dir prefix "" 10))
  ([prefix suffix]       (temp-dir prefix suffix 10))
  ([prefix suffix tries] (temp-create prefix suffix tries mkdir)))

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
  ([pattern]
     (let [parts (split pattern)
           root (apply file (if (= (count parts) 1) ["."] (butlast parts)))]
       (glob root (last parts))))
  ([^File root pattern]
     (let [regex (glob->regex pattern)]
       (seq (.listFiles
             root
             (reify FilenameFilter
               (accept [_ _ filename]
                 (boolean (re-find regex filename)))))))))

(defn- iterzip
  "Iterate over a zip, returns a sequence of the nodes with a nil suffix"
  [z]
  (when-not (zip/end? z)
    (cons (zip/node z) (lazy-seq (iterzip (zip/next z))))))

(defn- f-dir? [^File f]
  (when f (.isDirectory f)))

(defn- f-children [^File f]
  (.listFiles f))

(defn- iterate-dir* [path]
  (let [root (file path)
        nodes (butlast (iterzip (zip/zipper f-dir? f-children nil root)))]
    (filter f-dir? nodes)))

(defn- walk-map-fn [root]
  (let [kids (f-children root)
        dirs (set (map base-name (filter f-dir? kids)))
        files (set (map base-name (filter (complement f-dir?) kids)))]
    [root dirs files]))

(defn iterate-dir
  "Return a sequence [root dirs files], starting from 'path' in depth-first order"
  [path]
  (map walk-map-fn (iterate-dir* path)))

(defn walk
  "Lazily walk depth-first over the directory structure starting at
  'path' calling 'func' with three arguments [root dirs files].
  Returns a sequence of the results."
  [func path]
  (map #(apply func %) (iterate-dir path)))

(defn touch
  "Set file modification time (default to now). Returns path."
  [path & [time]]
  (let [f (file path)]
    (when-not (create f)
      (.setLastModified f (or time (System/currentTimeMillis))))
    f))

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
          f (file path)
          flag (= op "+")
          user (not (empty? u))]
      (when (perm-set \r) (.setReadable f flag user))
      (when (perm-set \w) (.setWritable f flag user))
      (when (perm-set \x) (.setExecutable f flag user)))
    path))

(defn copy+
  "Copy src to dest, create directories if needed."
  [src dest]
  (mkdirs (parent dest))
  (copy src dest))

(defn copy-dir
  "Copy a directory from 'from' to 'to'. If 'to' already exists, copy the directory
   to a directory with the same name as 'from' within the 'to' directory."
  [from to]
  (when (exists? from)
    (if (file? to)
      (throw (IllegalArgumentException. (str to " is a file")))
      (let [from (file from)
            to (if (exists? to)
                 (file to (base-name from))
                 (file to))
            trim-size (-> from str count inc)
            dest #(file to (subs (str %) trim-size))]
        (mkdirs to)
        (dorun
         (walk (fn [root dirs files]
                 (doseq [dir dirs]
                   (when-not (directory? dir)
                     (-> root (file dir) dest mkdirs)))
                 (doseq [f files]
                   (copy+ (file root f) (dest (file root f)))))
               from))
        to))))

(defn delete-dir
  "Delete a directory tree."
  [root]
  (when (directory? root)
    (doseq [path (map #(file root %) (.list (file root)))]
      (delete-dir path)))
  (delete root))

(defn parents
  "Get all the parent directories of a path."
  [f]
  (when-let [parent (parent (file f))]
    (cons parent (lazy-seq (parents parent)))))

(defn child-of?
  "Takes two paths and checks to see if the first path is a parent
   of the second."
  [p c] (some #{(file p)} (parents c)))

(defn ns-path
  "Takes a namespace symbol and creates a path to it. Replaces hyphens with
   underscores. Assumes the path should be relative to cwd."
  [n]
  (file
   (str (.. (str n)
            (replace \- \_)
            (replace \. \/))
        ".clj")))

(defn path-ns
  "Takes a path to a Clojure file and constructs a namespace symbol
   out of the path."
  [path]
  (symbol
   (.. (.replaceAll (str path) "\\.clj" "")
       (replace \_ \-)
       (replace \/ \.))))

(defn find-files*
  "Find files in path by pred."
  [path pred]
  (filter pred (-> path file file-seq)))

(defn find-files
  "Find files matching given pattern."
  [path pattern]
  (find-files* path #(re-matches pattern (.getName %))))

(defn exec
  "Execute a shell command in the current directory"
  [& body]
  (sh/with-sh-dir *cwd* (apply sh/sh body)))

(defmacro with-cwd
  "Execute body with a changed working directory."
  [cwd & body]
  `(binding [*cwd* (file ~cwd)]
     ~@body))

(defmacro with-mutable-cwd
  "Execute the body in a binding with *cwd* bound to *cwd*.
   This allows you to change *cwd* with set!."
  [& body]
  `(binding [*cwd* *cwd*]
     ~@body))

(defn chdir
  "set!s the value of *cwd* to path. Only works inside of
   with-mutable-cwd"
  [path]
  (set! *cwd* (file path)))
