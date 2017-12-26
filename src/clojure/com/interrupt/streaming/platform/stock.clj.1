(ns com.interrupt.streaming.platform.stock
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:stock :analytics]
   [:analytics :predictive-analytics]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :stock
                       :output-name :predictive-analytics
                       :function-name :analytics
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))
