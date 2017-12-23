(ns com.interrupt.streaming.platform.filtered-stocks
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:filtered-stocks :analytics]
   [:analytics :stock-command]])

(defn lifecycles [platform-type]
  ({:kafka []
    :onyx []}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url platform-type]

  (fn [topic]

    (cond

      (some #{:input-filtered-stocks
              :input-stock
              :input-stock-command-result}
            [topic])

      {:kafka {:onyx/medium :kafka
               :onyx/plugin :onyx.plugin.kafka/read-messages
               :kafka/wrap-with-metadata? true
               :kafka/zookeeper zookeeper-url
               :kafka/topic "foo"
               :kafka/key-deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-key
               :kafka/deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-message
               :kafka/offset-reset :earliest}

       :onyx {:onyx/medium :core.async
              :onyx/plugin :onyx.plugin.core-async/input}}

      (some #{:output-stock-command
              :output-predictive-analytics}
            [topic])

      {:kafka {:onyx/medium :kafka
               :onyx/plugin :onyx.plugin.kafka/write-messages
               :kafka/zookeeper zookeeper-url
               :kafka/topic "foo"
               :kafka/key-serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-key
               :kafka/serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-message
               :kafka/request-size 307200}

       :onyx {:onyx/medium :core.async
              :onyx/plugin :onyx.plugin.core-async/output}})))

(def input-filtered-stocks
  {:onyx/name :filtered-stocks
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'filtered-stocks' Kafka topic"})

(def input-stock
  {:onyx/name :stock
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'stock' Kafka topic"})

(def input-stock-command-result
  {:onyx/name :stock-command-result
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'stock-command-result' Kafka topic"})

(def function-analytics
  {:onyx/name :analytics
   :onyx/type :function
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/fn :com.interrupt.streaming.platform.base/local-identity})

(def output-stock-command
  {:onyx/name :stock-command
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-predictive-analytics
  {:onyx/name :predictive-analytics
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(defn catalog [zookeeper-url platform-type]
  [(merge input-filtered-stocks
          (-> (catalog-configs zookeeper-url platform-type)
              :input-filtered-stocks
              platform-type
              (assoc :kafka/topic "filtered-stocks")))

   (merge input-stock
          (-> (catalog-configs zookeeper-url platform-type)
              :input-stock
              platform-type
              (assoc :kafka/topic "stock")))

   (merge input-stock-command-result
          (-> (catalog-configs zookeeper-url platform-type)
              :input-stock-command-result
              (assoc :kafka/topic "stock-command-result")))

   function-analytics

   (merge output-stock-command
          (-> (catalog-configs zookeeper-url platform-type)
              :output-stock-command
              platform-type
              (assoc :kafka/topic "stock-command")))

   (merge output-predictive-analytics
          (-> (catalog-configs zookeeper-url platform-type)
              :output-predictive-analytics
              platform-type
              (assoc :kafka/topic "predictive-analytics")))])


(comment

  (catalog "foo:2181" :kafka))
