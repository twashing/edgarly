(ns com.interrupt.streaming.platform.trades
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:trades :bookeeping]
   [:bookeeping :positions]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :trades
                       :output-name :positions
                       :function-name :bookeeping
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))
