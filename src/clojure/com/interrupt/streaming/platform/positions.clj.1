(ns com.interrupt.streaming.platform.positions
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:positions :edgarly]
   [:edgarly :scanner-command]])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :positions
                       :output-name :scanner-command
                       :function-name :edgarly
                       :function-id :com.interrupt.streaming.platform.base/local-identity}))
