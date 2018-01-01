(ns com.interrupt.streaming.edgarly
  (:require [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:scanner-command-result :edgarly]
   [:start-trading :edgarly]
   [:stop-trading :edgarly]
   [:positions :edgarly]

   [:edgarly :scanner-command]
   [:edgarly :start-trading-result]
   [:edgarly :stop-trading-result]])

(defn lifecycles [platform-type]
  ({:kafka []
    :onyx []}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url]
  (fn [topic]
    (cond
      (some #{:scanner-command
              :start-trading
              :stop-trading}
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

      (some #{:scanner-command-result
              :start-trading-result
              :stop-trading-result
              :positions}
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

(def input-scanner-command-result
  {:onyx/name :scanner-command-result
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'scanner-command-result' Kafka topic"})

(def input-start-trading
  {:onyx/name :start-trading
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'start-trading' Kafka topic"})

(def input-stop-trading
  {:onyx/name :stop-trading
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'stop-trading' Kafka topic"})

(def input-positions
  {:onyx/name :positions
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'positions' Kafka topic"})

(def function-bookeeping
  {:onyx/name :bookeeping
   :onyx/type :function
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/fn :com.interrupt.streaming.platform.base/local-identity})

(def output-scanner-command
  {:onyx/name :scanner-command
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-start-trading-result
  {:onyx/name :start-trading-result
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-stop-trading-result
  {:onyx/name :stop-trading-result
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})


(defn catalog [zookeeper-url platform-type]
  [(merge input-scanner-command-result
          (-> ((catalog-configs zookeeper-url)
               :scanner-command-result)
              platform-type
              (assoc :kafka/topic "scanner-command-result")))

   (merge input-start-trading
          (-> ((catalog-configs zookeeper-url)
               :start-trading)
              platform-type
              (assoc :kafka/topic "start-trading")))

   (merge input-stop-trading
          (-> ((catalog-configs zookeeper-url)
               :stop-trading)
              platform-type
              (assoc :kafka/topic "stop-trading")))

   (merge input-positions
          (-> ((catalog-configs zookeeper-url)
               :positions)
              platform-type
              (assoc :kafka/topic "positions")))

   function-bookeeping

   (merge output-scanner-command
          (-> ((catalog-configs zookeeper-url)
               :scanner-command)
              platform-type
              (assoc :kafka/topic "scanner-command")))

   (merge output-start-trading-result
          (-> ((catalog-configs zookeeper-url)
               :start-trading-result)
              platform-type
              (assoc :kafka/topic "start-trading-result")))

   (merge output-stop-trading-result
          (-> ((catalog-configs zookeeper-url)
               :stop-trading-result)
              platform-type
              (assoc :kafka/topic "stop-trading-result")))])
