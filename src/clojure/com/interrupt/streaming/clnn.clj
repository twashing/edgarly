(ns com.interrupt.streaming.clnn
  (:require [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:predictive-analytics :clnn]
   [:filtered-stocks :clnn]
   [:historical :clnn]
   [:historical-command-result :clnn]
   [:clnn :historical-command]
   [:clnn :trade-recommendations]])

(defn lifecycles [platform-type]
  ({:kafka []
    :onyx []}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url]
  (fn [topic]
    (cond
      (some #{:input-predictive-analytics
              :input-filtered-stocks
              :input-historical
              :input-historical-command-result}
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

      (some #{:output-historical-command
              :output-trade-recommendations}
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

(def input-predictive-analytics
  {:onyx/name :predictive-analytics
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'predictive-analytics' Kafka topic"})

(def input-filtered-stocks
  {:onyx/name :filtered-stocks
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'filtered-stocks' Kafka topic"})

(def input-historical
  {:onyx/name :historical
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'historical' Kafka topic"})

(def input-historical-command-result
  {:onyx/name :historical-command-result
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'historical-command-result' Kafka topic"})

(def function-clnn
  {:onyx/name :clnn
   :onyx/type :function
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/fn :com.interrupt.streaming.platform.base/local-identity})

(def output-historical-command
  {:onyx/name :historical-command
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-trade-recommendations
  {:onyx/name :trade-recommendations
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})


(defn catalog [zookeeper-url platform-type]
  [(merge input-predictive-analytics
          (-> ((catalog-configs zookeeper-url)
               :input-predictive-analytics)
              platform-type
              (assoc :kafka/topic "predictive-analytics")))

   (merge input-filtered-stocks
          (-> ((catalog-configs zookeeper-url)
               :input-filtered-stocks)
              platform-type
              (assoc :kafka/topic "filtered-stocks")))

   (merge input-historical
          (-> ((catalog-configs zookeeper-url)
               :input-historical)
              platform-type
              (assoc :kafka/topic "historical")))

   (merge input-historical-command-result
          (-> ((catalog-configs zookeeper-url)
               :input-historical-command-result)
              platform-type
              (assoc :kafka/topic "historical-command-result")))

   function-clnn

   (merge output-historical-command
          (-> ((catalog-configs zookeeper-url)
               :output-historical-command)
              platform-type
              (assoc :kafka/topic "historical-command")))

   (merge output-trade-recommendations
          (-> ((catalog-configs zookeeper-url)
               :output-trade-recommendations)
              platform-type
              (assoc :kafka/topic "trade-recommendations")))])
