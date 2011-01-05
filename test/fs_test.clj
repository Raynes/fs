(ns fs-test
  (:use [fs] :reload-all)
  (:use [clojure.test]))

(deftest listdir-test
  (is (not (empty? (fs/listdir ".")))))

(deftest executable?-test
  (is (fs/executable? ".")))
