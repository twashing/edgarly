(ns com.interrupt.streaming.platform.scanner
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:scanner :market-scanner]
   [:market-scanner :filtered-stocks]])

(def input-topics
  [(ffirst workflow)])

(def output-topics
  [(-> workflow last last)])

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :scanner
                       :output-name :filtered-stocks
                       :function-name :market-scanner
                       :function-id :com.interrupt.streaming.platform.base/local-identity})) 
