(ns com.interrupt.streaming.platform
  (:require [onyx.test-helper :refer [with-test-env load-config]]
            [onyx.plugin.kafka]
            [onyx.api])
  (:import [java.util UUID]))


(def zookeeper-url "zookeeper:2181")
(def kafka-url "kafka:9092")

(def topic-scanner-command "scanner-command")
(def topic-scanner "scanner")
(def topic-filtered-stocks "filtered-stocks")
(def topic-stock-command "stock-command")

(defn find-task [catalog task-name]
  (let [matches (filter #(= task-name (:onyx/name %)) catalog)]
    (when-not (seq matches)
      (throw (ex-info (format "Couldn't find task %s in catalog" task-name)
                      {:catalog catalog :task-name task-name})))
    (first matches)))

(defn n-peers
  "Takes a workflow and catalog, returns the minimum number of peers
   needed to execute this job."
  [catalog workflow]
  (let [task-set (into #{} (apply concat workflow))]
    (reduce
     (fn [sum t]
       (+ sum (or (:onyx/min-peers (find-task catalog t)) 1)))
     0 task-set)))
