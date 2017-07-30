(ns com.interrupt.streaming.platform.historical
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:historical :clnn]
   [:clnn :trade-recommendations]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :historical
                       :output-name :trade-recommendations
                       :function-name :clnn
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))
