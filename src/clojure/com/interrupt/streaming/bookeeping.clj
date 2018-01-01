(ns com.interrupt.streaming.bookeeping
  (:require [com.interrupt.streaming.platform.serialization]))


(def workflow
  [[:trade-command :bookeeping]
   #_[:bookeeping :trade-command-result]
   [:bookeeping :positions]])


(defn lifecycles [platform-type]
  ({:kafka []
    :onyx []}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url]
  (fn [topic]
    (cond
      (some #{:input-trade-command}
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

      (some #{:output-positions
              :output-trade-command-result}
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


(def input-trade-command
  {:onyx/name :trade-command
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'trade-recommendations' Kafka topic"})

(def function-bookeeping
  {:onyx/name :bookeeping
   :onyx/type :function
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/fn :com.interrupt.streaming.platform.base/local-identity})

(def output-trade-command-result
  {:onyx/name :trade-command-result
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-positions
  {:onyx/name :positions
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})


(defn catalog [zookeeper-url platform-type]
  [(merge input-trade-command
          (-> ((catalog-configs zookeeper-url)
               :input-trade-command)
              platform-type
              (assoc :kafka/topic "trade-command")))

   function-bookeeping

   (merge output-trade-command-result
          (-> ((catalog-configs zookeeper-url)
               :output-trade-command-result)
              platform-type
              (assoc :kafka/topic "trade-command-result")))

   (merge output-positions
          (-> ((catalog-configs zookeeper-url)
               :output-positions)
              platform-type
              (assoc :kafka/topic "positions")))])
