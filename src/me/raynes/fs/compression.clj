(ns me.raynes.fs.compression
  "Compression utilities."
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs])
  (:import (java.util.zip ZipFile GZIPInputStream)
           (org.apache.commons.compress.archivers.tar TarArchiveInputStream
                                                      TarArchiveEntry)
           (org.apache.commons.compress.compressors bzip2.BZip2CompressorInputStream
                                                    xz.XZCompressorInputStream)
           (java.io ByteArrayOutputStream File)))

(defn- check-final-path-inside-target-dir! [f target-dir entry]
  (when-not (-> f .getCanonicalPath (.startsWith (str (.getCanonicalPath target-dir) File/separator)))
    (throw (ex-info "Expanding entry would be created outside target dir"
                    {:entry entry
                     :entry-final-path f
                     :target-dir target-dir}))))

(defn unzip
  "Takes the path to a zipfile `source` and unzips it to target-dir."
  ([source]
     (unzip source (name source)))
  ([source target-dir]
     (with-open [zip (ZipFile. (fs/file source))]
       (let [entries (enumeration-seq (.entries zip))
             target-dir-as-file (fs/file target-dir)
             target-file #(fs/file target-dir (str %))]
         (doseq [entry entries :when (not (.isDirectory ^java.util.zip.ZipEntry entry))
                 :let [^File f (target-file entry)]]
           (check-final-path-inside-target-dir! f target-dir-as-file entry)
           (fs/mkdirs (fs/parent f))
           (io/copy (.getInputStream zip entry) f))))
     target-dir))

(defn- add-zip-entry
  "Add a zip entry. Works for strings and byte-arrays."
  [^java.util.zip.ZipOutputStream zip-output-stream [^String name content & remain]]
  (.putNextEntry zip-output-stream (java.util.zip.ZipEntry. name))
  (if (string? content) ;string and byte-array must have different methods
    (doto (java.io.PrintStream. zip-output-stream true)
      (.print content))
    (.write zip-output-stream ^bytes content))
  (.closeEntry zip-output-stream)
  (when (seq (drop 1 remain))
    (recur zip-output-stream remain)))

(defn make-zip-stream
  "Create zip file(s) stream. You must provide a vector of the
  following form: 

  ```[[filename1 content1][filename2 content2]...]```.

  You can provide either strings or byte-arrays as content.

  The piped streams are used to create content on the fly, which means
  this can be used to make compressed files without even writing them
  to disk."
  [& filename-content-pairs]
  (let [file
    (let [pipe-in (java.io.PipedInputStream.)
          pipe-out (java.io.PipedOutputStream. pipe-in)]
      (future
        (with-open [zip (java.util.zip.ZipOutputStream. pipe-out)]
          (add-zip-entry zip (flatten filename-content-pairs))))
      pipe-in)]
    (io/input-stream file)))

(defn zip
  "Create zip file(s) on the fly. You must provide a vector of the
  following form: 

  ```[[filename1 content1][filename2 content2]...]```.

  You can provide either strings or byte-arrays as content."
  [filename & filename-content-pairs]
  (io/copy (make-zip-stream filename-content-pairs)
           (fs/file filename)))

(defn- slurp-bytes [fpath]
  (with-open [data (io/input-stream (fs/file fpath))]
    (with-open [out (ByteArrayOutputStream.)]
      (io/copy data out)
      (.toByteArray out))))

(defn make-zip-stream-from-files
  "Like make-zip-stream but takes a sequential of file paths and builds filename-content-pairs
   based on those"
  [fpaths]
  (let [filename-content-pairs (map (juxt fs/base-name slurp-bytes) fpaths)]
    (make-zip-stream filename-content-pairs)))

(defn zip-files
  "Zip files provided in argument vector to a single zip. Converts the argument list:

  ```(fpath1 fpath2...)```

  into filename-content -pairs, using the original file's basename as the filename in zip`and slurping the content:

  ```([fpath1-basename fpath1-content] [fpath2-basename fpath2-content]...)``"
  [filename fpaths]
  (io/copy (make-zip-stream-from-files fpaths)
           (fs/file filename)))

(defn- tar-entries
  "Get a lazy-seq of entries in a tarfile."
  [^TarArchiveInputStream tin]
  (when-let [entry (.getNextTarEntry tin)]
    (cons entry (lazy-seq (tar-entries tin)))))

(defn untar
  "Takes a tarfile `source` and untars it to `target`."
  ([source] (untar source (name source)))
  ([source target]
     (with-open [tin (TarArchiveInputStream. (io/input-stream (fs/file source)))]
       (let [target-dir-as-file (fs/file target)]
         (doseq [^TarArchiveEntry entry (tar-entries tin) :when (not (.isDirectory entry))
                 :let [output-file (fs/file target (.getName entry))]]
           (check-final-path-inside-target-dir! output-file target-dir-as-file entry)
           (fs/mkdirs (fs/parent output-file))
           (io/copy tin output-file)
           (when (.isFile entry)
             (fs/chmod (apply str (take-last
                                    3 (format "%05o" (.getMode entry))))
                       (.getPath output-file))))))))

(defn gunzip
  "Takes a path to a gzip file `source` and unzips it."
  ([source] (gunzip source (name source)))
  ([source target]
     (io/copy (-> source fs/file io/input-stream GZIPInputStream.)
              (fs/file target))))

(defn bunzip2
  "Takes a path to a bzip2 file `source` and uncompresses it."
  ([source] (bunzip2 source (name source)))
  ([source target]
     (io/copy (-> source fs/file io/input-stream BZip2CompressorInputStream.)
              (fs/file target))))

(defn unxz
  "Takes a path to a xz file `source` and uncompresses it."
  ([source] (unxz source (name source)))
  ([source target]
    (io/copy (-> source fs/file io/input-stream XZCompressorInputStream.)
             (fs/file target))))
