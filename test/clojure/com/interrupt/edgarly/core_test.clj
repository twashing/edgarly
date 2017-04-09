(ns clojure.com.interrupt.edgarly.core-test
  (:require [clojure.test :as t]
            [com.interrupt.edgarly.core :as sut]
            [com.interrupt.test :refer [defspec-test] :as it]))


(defspec-test test-core [sut/average sut/next-reqid])

