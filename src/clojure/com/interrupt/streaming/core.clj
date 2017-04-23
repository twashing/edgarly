(ns com.interrupt.streaming.core
  (:require [onyx.test-helper :refer [with-test-env load-config]]
            [onyx.plugin.kafka]
            [onyx.api]
            [franzy.clients.producer.client :as producer]
            [franzy.clients.consumer.client :as consumer]
            [franzy.clients.producer.protocols :refer :all]
            [franzy.clients.consumer.protocols :refer :all]
            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]
            [franzy.admin.zookeeper.client :as client]
            [franzy.admin.topics :as topics]
            [franzy.clients.producer.defaults :as pd])
  (:import [java.util UUID]))


(def zookeeper-url "edgarly_zookeeper_1:2181")
(def kafka-url "edgarly_kafka_1:9092")

(def topic-scanner-command "scanner-command")
(def topic-scanner "scanner")

(def key-serializer (serializers/keyword-serializer))
(def value-serializer (serializers/edn-serializer))

(defn one-setup-topics []
  (def zk-utils (client/make-zk-utils {:servers zookeeper-url} false))
  (def two (topics/create-topic! zk-utils topic-scanner-command 10))
  (def three (topics/create-topic! zk-utils topic-scanner 10))
  (topics/all-topics zk-utils))

(defn two-write-to-topic []
  (let [;; Use a vector if you wish for multiple servers in your cluster
        pc {:bootstrap.servers [kafka-url]}

        ;;Serializes producer record keys that may be keywords
        key-serializer (serializers/keyword-serializer)

        ;;Serializes producer record values as EDN, built-in
        value-serializer (serializers/edn-serializer)

        ;;optionally create some options, even just use the defaults explicitly
        ;;for those that don't need anything fancy...
        options (pd/make-default-producer-options)
        topic topic-scanner-command
        partition 0]

    (with-open [p (producer/make-producer pc key-serializer value-serializer options)]
      (let [send-fut (send-async! p topic partition :a {:foo :bar} options)]
        (println "Async send results:" @send-fut)))))

(def workflow
  [[:read-commands :process-commands]
   [:process-commands :write-messages]])

(def printer (agent nil))
(defn echo-segments [event lifecycle]
  (send printer
        (fn [_]
          (doseq [segment (:onyx.core/batch event)]
            (println (format "Peer %s saw segment %s"
                             (:onyx.core/id event)
                             segment)))))
  {})

(def identity-lifecycle
  {:lifecycle/after-batch echo-segments})

(defn catalog [zookeeper-url topic-read topic-write]
  [{:onyx/name :read-commands
    :onyx/type :input
    :onyx/medium :kafka
    :onyx/plugin :onyx.plugin.kafka/read-messages
    :onyx/max-peers 1
    :onyx/batch-size 50
    :kafka/zookeeper zookeeper-url
    :kafka/topic topic-read
    :kafka/deserializer-fn :franzy.serialization.deserializers/edn-deserializer
    :kafka/offset-reset :earliest
    :onyx/doc "Read commands from a Kafka topic"}

   {:onyx/name :process-commands
    :onyx/type :function
    :onyx/max-peers 1
    :onyx/batch-size 50
    :onyx/fn :clojure.core/identity}

   {:onyx/name :write-messages
    :onyx/type :output
    :onyx/medium :kafka
    :onyx/plugin :onyx.plugin.kafka/write-messages
    :onyx/max-peers 1
    :onyx/batch-size 50
    :kafka/zookeeper zookeeper-url
    :kafka/topic topic-write
    :kafka/serializer-fn :franzy.serialization.serializers/edn-serializer
    :kafka/request-size 307200
    :onyx/doc "Writes messages to a Kafka topic"}])

(defn build-lifecycles []
  [{:lifecycle/task :process-commands
    :lifecycle/calls :com.interrupt.streaming.core/identity-lifecycle
    :onyx/doc "Lifecycle for logging segments"}])

(comment

  ;; 1
  (one-setup-topics)

  ;; 2
  (two-write-to-topic)

  ;; 3
  (let [tenancy-id (UUID/randomUUID)
        config (load-config "dev-config.edn")
        env-config (assoc (:env-config config)
                          :onyx/tenancy-id tenancy-id
                          :zookeeper/address zookeeper-url)
        peer-config (assoc (:peer-config config)
                           :onyx/tenancy-id tenancy-id
                           :zookeeper/address zookeeper-url)
        job {:workflow workflow
             :catalog (catalog zookeeper-url topic-scanner-command topic-scanner)
             ;; :flow-conditions c/flow-conditions
             :lifecycles (build-lifecycles)
             ;; :windows c/windows
             ;; :triggers c/triggers
             :task-scheduler :onyx.task-scheduler/balanced}]

    ;; (make-topic! kafka-zookeeper commands-topic)
    ;; (make-topic! kafka-zookeeper events-topic)
    ;; (write-commands! kafka-brokers commands-topic commands)

    (with-test-env [test-env [3 env-config peer-config]]
      (onyx.api/submit-job peer-config job))))
