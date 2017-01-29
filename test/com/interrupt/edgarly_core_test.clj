(ns com.interrupt.edgarly-core-test
  (:require [clojure.test :refer :all]
            [com.interrupt.edgarly-core :refer :all]))

(deftest hello-test
  (testing "says hello to caller"
    (is (= "Hello, foo!" (hello "foo")))))
