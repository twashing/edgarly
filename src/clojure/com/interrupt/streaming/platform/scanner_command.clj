(ns com.interrupt.streaming.platform.scanner-command
  (:require [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:scanner-command :ibgateway]
   [:ibgateway :scanner-command-result]
   [:ibgateway :scanner]])

(def input-topics
  [(ffirst workflow)])

(def output-topics
  (->> workflow rest (map last)))

(defn catalog [zookeeper-url topic-read topic-write]
  (base/catalog-basic zookeeper-url topic-read topic-write
                      {:input-name :scanner-command
                       :output-name :scanner-command-result
                       :function-name :ibgateway
                       :function-id :com.interrupt.streaming.platform.base/local-identity})

  [{:onyx/name :scanner-command
    :onyx/type :input
    :onyx/medium :kafka
    :onyx/plugin :onyx.plugin.kafka/read-messages
    :kafka/wrap-with-metadata? true
    :onyx/min-peers 1
    :onyx/max-peers 1
    :onyx/batch-size 10
    :kafka/zookeeper zookeeper-url
    :kafka/topic topic-read
    :kafka/deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-message
    :kafka/key-deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-key
    :kafka/offset-reset :earliest
    :onyx/doc "Read from the 'scanner-command' Kafka topic"}

   {:onyx/name :ibgateway
    :onyx/type :function
    :onyx/min-peers 1
    :onyx/max-peers 1
    :onyx/batch-size 10
    :onyx/fn :com.interrupt.streaming.platform.base/local-identity}

   {:onyx/name :scanner-command-result
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
    :onyx/doc "Writes messages to a Kafka topic"}

   {:onyx/name :scanner
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
