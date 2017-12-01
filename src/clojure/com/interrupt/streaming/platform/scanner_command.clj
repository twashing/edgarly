(ns com.interrupt.streaming.platform.scanner-command
  (:require [clojure.core.async :refer [chan]]
            [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


;; WORKFLOW
(def workflow
  [[:scanner-command :ibgateway]
   [:ibgateway :scanner-command-result]
   [:ibgateway :scanner]])

(def input-topics
  [(ffirst workflow)])

(def output-topics
  (->> workflow rest (map last)))


;; LIFECYCLES
(def capacity 500)
(def in-chan (chan capacity))
(def in-buffer (atom {}))
(def out-chan (chan capacity))

(defn inject-in-ch [event lifecycle]
  {:core.async/buffer in-buffer
   :core.async/chan in-chan})

(defn inject-out-ch [event lifecycle]
  {:core.async/chan out-chan})

(def in-calls {:lifecycle/before-task-start inject-in-ch})
(def out-calls {:lifecycle/before-task-start inject-out-ch})

(defn lifecycles [platform-type]
  ({:kafka []
    :onyx [{:lifecycle/task :scanner-command
            :lifecycle/calls :com.interrupt.streaming.platform.scanner-command/in-calls}
           {:lifecycle/task :scanner-command
            :lifecycle/calls :onyx.plugin.core-async/reader-calls}

           {:lifecycle/task :scanner-command-result
            :lifecycle/calls :com.interrupt.streaming.platform.scanner-command/out-calls}
           {:lifecycle/task :scanner-command-result
            :lifecycle/calls :onyx.plugin.core-async/writer-calls}

           {:lifecycle/task :scanner
            :lifecycle/calls :com.interrupt.streaming.platform.scanner-command/out-calls}
           {:lifecycle/task :scanner
            :lifecycle/calls :onyx.plugin.core-async/writer-calls}]}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url topic-read platform-type]
  {:input-scanner-command
   {:kafka {:onyx/medium :kafka
            :onyx/plugin :onyx.plugin.kafka/read-messages
            :kafka/wrap-with-metadata? true
            :kafka/zookeeper zookeeper-url
            :kafka/topic topic-read
            :kafka/key-deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-key
            :kafka/deserializer-fn :com.interrupt.streaming.platform.serialization/deserialize-kafka-message
            :kafka/offset-reset :earliest}

    :onyx {:onyx/medium :core.async
           :onyx/plugin :onyx.plugin.core-async/input}}


   :output-scanner-command-result
   {:kafka {:onyx/medium :kafka
            :onyx/plugin :onyx.plugin.kafka/write-messages
            :kafka/zookeeper zookeeper-url
            :kafka/topic "scanner-command-result"
            :kafka/key-serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-key
            :kafka/serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-message
            :kafka/request-size 307200}

    :onyx {:onyx/medium :core.async
           :onyx/plugin :onyx.plugin.core-async/output}}


   :output-scanner
   {:kafka {:onyx/medium :kafka
            :onyx/plugin :onyx.plugin.kafka/write-messages
            :kafka/zookeeper zookeeper-url
            :kafka/topic "scanner"
            :kafka/key-serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-key
            :kafka/serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-message
            :kafka/request-size 307200}

    :onyx {:onyx/medium :core.async
           :onyx/plugin :onyx.plugin.core-async/output}}})

(def input-scanner-command
  {:onyx/name :scanner-command
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'scanner-command' Kafka topic"})

(def function-ibgateway
  {:onyx/name :ibgateway
   :onyx/type :function
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/fn :com.interrupt.streaming.platform.base/local-identity})

(def output-scanner-command-result
  {:onyx/name :scanner-command-result
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})

(def output-scanner
  {:onyx/name :scanner
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to a Kafka topic"})


(defn catalog [zookeeper-url topic-read platform-type]

  [(merge input-scanner-command
          (-> (catalog-configs zookeeper-url topic-read platform-type)
              :input-scanner-command platform-type))

   function-ibgateway

   (merge output-scanner-command-result
          (-> (catalog-configs zookeeper-url topic-read platform-type)
              :output-scanner-command-result platform-type))

   (merge output-scanner
          (-> (catalog-configs zookeeper-url topic-read platform-type)
              :output-scanner platform-type))])
