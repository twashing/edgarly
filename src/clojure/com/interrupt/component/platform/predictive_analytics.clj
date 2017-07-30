(ns com.interrupt.streaming.platform.predictive-analytics
  (:require [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:predictive-analytics :clnn]
   [:filtered-stocks :clnn]
   [:clnn :historical-command]])

(defn catalog [zookeeper-url topic-read-left topic-read-right topic-write]

  [{:onyx/name :predictive-analytics
    :onyx/type :input
    :onyx/medium :kafka
    :onyx/plugin :onyx.plugin.kafka/read-messages
    :kafka/wrap-with-metadata? true
    :onyx/min-peers 1
    :onyx/max-peers 1
    :onyx/batch-size 10
    :kafka/zookeeper zookeeper-url
    :kafka/topic topic-read-left
    :kafka/deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-message
    :kafka/key-deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-key
    :kafka/offset-reset :earliest
    :onyx/doc "Read from the 'scanner-command' Kafka topic"}

   {:onyx/name :filtered-stocks
    :onyx/type :input
    :onyx/medium :kafka
    :onyx/plugin :onyx.plugin.kafka/read-messages
    :kafka/wrap-with-metadata? true
    :onyx/min-peers 1
    :onyx/max-peers 1
    :onyx/batch-size 10
    :kafka/zookeeper zookeeper-url
    :kafka/topic topic-read-right
    :kafka/deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-message
    :kafka/key-deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-key
    :kafka/offset-reset :earliest
    :onyx/doc "Read from the 'scanner-command' Kafka topic"}

   {:onyx/name :clnn
    :onyx/type :function
    :onyx/min-peers 1
    :onyx/max-peers 1
    :onyx/batch-size 10
    :onyx/fn :com.interrupt.streaming.platform.base/local-identity}

   {:onyx/name :historical-command
    :onyx/type :output
    :onyx/medium :kafka
    :onyx/plugin :onyx.plugin.kafka/write-messages
    :onyx/min-peers 1
    :onyx/max-peers 1
    :onyx/batch-size 10
    :kafka/zookeeper zookeeper-url
    :kafka/topic topic-write
    :kafka/serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-message
    :kafka/key-serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-key
    :kafka/request-size 307200
    :onyx/doc "Writes messages to a Kafka topic"}])
