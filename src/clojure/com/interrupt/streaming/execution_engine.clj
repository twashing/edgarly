(ns com.interrupt.streaming.execution-engine
  (:require [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:trade-recommendations :execution-engine]
   [:trade-command-result :execution-engine]
   [:start-trading-result :execution-engine]
   [:stop-trading-result :execution-engine]
   [:positions :execution-engine]
   [:execution-engine :trade-command]
   [:execution-engine :start-trading]
   [:execution-engine :stop-trading]])


(defn lifecycles [platform-type]
  ({:kafka []
    :onyx []}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url]
  (fn [topic]
    (cond
      (some #{:input-trade-recommendations
              :input-trade-command-result
              :input-start-trading-result
              :input-stop-trading-result
              :input-positions}
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

      (some #{:output-trade-command
              :output-start-trading
              :output-stop-trading}
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


(def input-trade-recommendations
  {:onyx/name :trade-recommendations
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'trade-recommendations' Kafka topic"})

(def input-trade-command-result
  {:onyx/name :trade-command-result
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'trade-command-result' Kafka topic"})

(def input-start-trading-result
  {:onyx/name :start-trading-result
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the ':start-trading-result' Kafka topic"})

(def input-stop-trading-result
  {:onyx/name :stop-trading-result
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'stop-trading-result' Kafka topic"})

(def input-positions
  {:onyx/name :positions
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'positions' Kafka topic"})

(def function-execution-engine
  {:onyx/name :execution-engine
   :onyx/type :function
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/fn :com.interrupt.streaming.platform.base/local-identity})

(def output-trade-command
  {:onyx/name :trade-command
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-start-trading
  {:onyx/name :start-trading
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-stop-trading
  {:onyx/name :stop-trading
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})


(defn catalog [zookeeper-url platform-type]
  [(merge input-trade-recommendations
          (-> ((catalog-configs zookeeper-url)
               :input-trade-recommendations)
              platform-type
              (assoc :kafka/topic "trade-recommendations")))

   (merge input-trade-command-result
          (-> ((catalog-configs zookeeper-url)
               :input-trade-command-result)
              platform-type
              (assoc :kafka/topic "trade-command-result")))

   (merge input-start-trading-result
          (-> ((catalog-configs zookeeper-url)
               :input-start-trading-result)
              platform-type
              (assoc :kafka/topic "start-trading-result")))

   (merge input-stop-trading-result
          (-> ((catalog-configs zookeeper-url)
               :input-stop-trading-result)
              platform-type
              (assoc :kafka/topic "stop-trading-result")))

   (merge input-positions
          (-> ((catalog-configs zookeeper-url)
               :input-positions)
              platform-type
              (assoc :kafka/topic "positions")))

   function-execution-engine

   (merge output-trade-command
          (-> ((catalog-configs zookeeper-url)
               :output-trade-command)
              platform-type
              (assoc :kafka/topic "trade-command")))

   (merge output-start-trading
          (-> ((catalog-configs zookeeper-url)
               :output-start-trading)
              platform-type
              (assoc :kafka/topic "start-trading")))

   (merge output-stop-trading
          (-> ((catalog-configs zookeeper-url)
               :output-stop-trading)
              platform-type
              (assoc :kafka/topic "stop-trading")))])
