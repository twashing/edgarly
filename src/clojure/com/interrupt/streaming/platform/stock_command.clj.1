(ns com.interrupt.streaming.platform.stock-command
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:stock-command :ibgateway]
   [:ibgateway :stock]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :stock-command
                       :output-name :stock
                       :function-name :ibgateway
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))
