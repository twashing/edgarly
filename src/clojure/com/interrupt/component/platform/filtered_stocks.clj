(ns com.interrupt.streaming.platform.filtered-stocks
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:filtered-stocks :analytics]
   [:analytics :stock-command]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :filtered-stocks
                       :output-name :stock-command
                       :function-name :analytics
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))
