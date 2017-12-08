(ns com.interrupt.streaming.platform
  (:require [onyx.test-helper :refer [with-test-env load-config]]
            [onyx.plugin.kafka]
            [onyx.api]
            
            [com.interrupt.streaming.platform.scanner-command :as psc]
            [com.interrupt.streaming.platform.scanner :as ps]
            [com.interrupt.streaming.platform.filtered-stocks :as pfs]
            [com.interrupt.streaming.platform.stock-command :as pstc]
            [com.interrupt.streaming.platform.stock :as pst]
            [com.interrupt.streaming.platform.predictive-analytics :as pa]
            [com.interrupt.streaming.platform.historical-command :as phc]
            [com.interrupt.streaming.platform.historical :as ph]
            [com.interrupt.streaming.platform.trade-recommendations :as ptr]
            [com.interrupt.streaming.platform.trades :as pt]
            [com.interrupt.streaming.platform.positions :as pp])
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

#_(comment

    ;; 1
    (one-setup-topics)

    ;; 2
    (two-write-to-topic "scanner-command" (str (UUID/randomUUID)) {:foo :bar})

    (let [config (load-config "dev-config.edn")
          env-config (assoc (:env-config config)
                            :zookeeper/address zookeeper-url)
          peer-config (assoc (:peer-config config)
                             :zookeeper/address zookeeper-url)
          env (onyx.api/start-env env-config)
          peer-group (onyx.api/start-peer-group peer-config)

          peer-count 6 #_(n-peers the-catalog the-workflow)
          v-peers (onyx.api/start-peers peer-count peer-group)]

      (let [the-workflow psc/workflow
            the-catalog (psc/catalog zookeeper-url "scanner-command" "scanner")
            job {:workflow the-workflow
                 :catalog the-catalog
                 :task-scheduler :onyx.task-scheduler/balanced}]
        (onyx.api/submit-job peer-config job))

      (let [the-workflow ps/workflow
            the-catalog (ps/catalog zookeeper-url "scanner" "filtered-stocks")
            job {:workflow the-workflow
                 :catalog the-catalog
                 :task-scheduler :onyx.task-scheduler/balanced}]
        (onyx.api/submit-job peer-config job))

      #_(let [the-workflow pfs/workflow
              the-catalog (pfs/catalog zookeeper-url "filtered-stocks" "stock-command")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow pstc/workflow
              the-catalog (pstc/catalog zookeeper-url "stock-command" "stock")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow pst/workflow
              the-catalog (pst/catalog zookeeper-url "stock" "predictive-analytics" )
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow pa/workflow
              the-catalog (pa/catalog zookeeper-url "predictive-analytics" "filtered-stocks" "historical-command")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow phc/workflow
              the-catalog (phc/catalog zookeeper-url "historical-command" "historical")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow ph/workflow
              the-catalog (ph/catalog zookeeper-url "historical" "trade-recommendations")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow ptr/workflow
              the-catalog (ptr/catalog zookeeper-url "trade-recommendations" "trades")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow pt/workflow
              the-catalog (pt/catalog zookeeper-url "trades" "positions")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))

      #_(let [the-workflow pp/workflow
              the-catalog (pp/catalog zookeeper-url "positions" "scanner-command")
              job {:workflow the-workflow
                   :catalog the-catalog
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job))))
