(ns com.interrupt.streaming.platform.trade-recommendations
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:trade-recommendations :execution-engine]
   [:execution-engine :trades]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :trade-recommendations
                       :output-name :trades
                       :function-name :execution-engine
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))

