(ns fs
  (:import java.io.File))

(def separator File/separator)

(defn listdir [path]
  (seq (.list (File. path))))

(defn executable? [path]
  (.canExecute (File. path)))

(defn readable? [path]
  (.canRead (File. path)))

(defn writeable? [path]
  (.canWrite (File. path)))

(defn delete [path]
  (.delete (File. path)))

(defn exists? [path]
  (.exists (File. path)))

(defn abspath [path]
  (.getAbsolutePath (File. path)))

(defn normpath [path]
  (.getCanonicalPath (File. path)))

(defn basename [path]
  (.getName (File. path)))

(defn dirname [path]
  (.getParent (File. path)))

(defn directory? [path]
  (.isDirectory (File. path)))

(defn file? [path]
  (.isFile (File. path)))

(defn mtime [path]
  (.lastModified (File. path)))

(defn size [path]
  (.length (File. path)))

(defn mkdir [path]
  (.mkdir (File. path)))

(defn mkdirs [path]
  (.mkdirs (File. path)))

(defn join [& parts]
  (apply str (interpose separator parts)))

(defn split [path]
  (into [] (.split path separator)))

(defn rename [old-path new-path]
  (.renameTo (File. old-path) (File. new-path)))

(defn tempfile 
  ([] (tempfile "-fs-" ""))
  ([prefix] (tempfile prefix ""))
  ([prefix suffix] (.getAbsolutePath (File/createTempFile prefix suffix)))
  ([prefix suffix directory] 
   (.getAbsolutePath (File/createTempFile prefix suffix (File. directory)))))

(defn tempdir
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

(defn cwd []
  (abspath "."))

; FIXME: ...
;(defn dir-seq [root]
