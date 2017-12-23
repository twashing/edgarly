(ns com.interrupt.streaming.platform.base
  (:require [clojure.core.async :refer [chan]]))


(def capacity 500)
(def chan-scanner-command (atom (chan capacity)))
(def chan-scanner-command-result (atom (chan capacity)))
(def chan-scanner (atom (chan capacity)))
(def chan-filtered-stocks (atom (chan capacity)))

(def chan-predictive-analytics (chan capacity))
(def chan-historical-command (chan capacity))
(def chan-historical-command-result (chan capacity))
(def chan-historical (chan capacity))
(def chan-trade-command (chan capacity))
(def chan-trade-command-result (chan capacity))
(def chan-trade-recommendations (chan capacity))
(def chan-positions (chan capacity))
(def chan-start-trading (chan capacity))
(def chan-start-trading-result (chan capacity))
(def chan-stop-trading (chan capacity))
(def chan-stop-trading-result (chan capacity))
(def chan-stock (chan capacity))
(def chan-stock-command (chan capacity))
(def chan-stock-command-result (chan capacity))


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
