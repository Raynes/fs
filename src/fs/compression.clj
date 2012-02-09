(ns fs.compression
  "Compression utilities."
  (:require [clojure.java.io :as io]
            [fs.core :as fs])
  (:import (java.util.zip ZipFile GZIPInputStream)
            (org.apache.commons.compress.archivers.tar TarArchiveInputStream
                                                       TarArchiveEntry)
            org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream))

(defn unzip
  "Takes the path to a zipfile source and unzips it to target-dir."
  ([source]
     (unzip source (name source)))
  ([source target-dir]
     (let [zip (ZipFile. (fs/file source))
           entries (enumeration-seq (.entries zip))
           target-file #(fs/file target-dir (str %))]
       (doseq [entry entries :when (not (.isDirectory ^java.util.zip.ZipEntry entry))
               :let [f (target-file entry)]]
         (fs/mkdirs (fs/parent f))
         (io/copy (.getInputStream zip entry) f)))
     target-dir))

(defn- tar-entries
  "Get a lazy-seq of entries in a tarfile."
  [^TarArchiveInputStream tin]
  (when-let [entry (.getNextTarEntry tin)]
    (cons entry (lazy-seq (tar-entries tin)))))

(defn untar
  "Takes a tarfile source and untars it to target."
  ([source] (untar source (name source)))
  ([source target]
     (with-open [tin (TarArchiveInputStream. (io/input-stream (fs/file source)))]
       (doseq [^TarArchiveEntry entry (tar-entries tin) :when (not (.isDirectory entry))
               :let [output-file (fs/file target (.getName entry))]]
         (fs/mkdirs (fs/parent output-file))
         (io/copy tin output-file)))))

(defn gunzip
  "Takes a path to a gzip file source and unzips it."
  ([source] (gunzip source (name source)))
  ([source target]
     (io/copy (-> source fs/file io/input-stream GZIPInputStream.)
              (fs/file target))))

(defn bunzip2
  "Takes a path to a bzip2 file source and uncompresses it."
  ([source] (bunzip2 source (name source)))
  ([source target]
     (io/copy (-> source fs/file io/input-stream BZip2CompressorInputStream.)
              (fs/file target))))
