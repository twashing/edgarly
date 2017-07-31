(ns com.interrupt.streaming.platform.historical-command
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:historical-command :ibgateway]
   [:ibgateway :historical]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :historical-command
                       :output-name :ibgateway
                       :function-name :historical
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))
