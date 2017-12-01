(ns com.interrupt.streaming.platform.base)

(defn catalog-basic [zookeeper-url topic-read topic-write
                     {:keys [input-name output-name
                             function-name function-id]}]
  [{:onyx/name input-name
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

   {:onyx/name function-name
    :onyx/type :function
    :onyx/min-peers 1
    :onyx/max-peers 1
    :onyx/batch-size 10
    :onyx/fn function-id}

   {:onyx/name output-name
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

(defn local-identity [segment]
  (println "local-identity segment: " segment)
  (dissoc segment :topic)) 
