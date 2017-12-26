(ns com.interrupt.streaming.platform.ibgateway
  (:require [clojure.core.async :refer [chan >!! <!! >! <!]]
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
(def in-buffer (atom {}))

#_(def chan-scanner-command (atom (chan 500)))
#_(def chan-scanner-command-result (atom (chan 500)))
#_(def chan-scanner (atom (chan 500)))

(defn inject-scanner-command-ch [event lifecycle]
  {:core.async/buffer in-buffer
   :core.async/chan @base/chan-scanner-command})
(defn inject-scanner-command-result-ch [event lifecycle] {:core.async/chan @base/chan-scanner-command-result})
(defn inject-scanner-ch [event lifecycle] {:core.async/chan @base/chan-scanner})

(def in-calls-scanner-command {:lifecycle/before-task-start inject-scanner-command-ch})
(def out-calls-scanner-command-result {:lifecycle/before-task-start inject-scanner-command-result-ch})
(def out-calls-scanner {:lifecycle/before-task-start inject-scanner-ch})

(defn lifecycles [platform-type]
  ({:kafka []
    :onyx [{:lifecycle/task :scanner-command
            :lifecycle/calls :com.interrupt.streaming.platform.scanner-command/in-calls-scanner-command}
           {:lifecycle/task :scanner-command
            :lifecycle/calls :onyx.plugin.core-async/reader-calls}

           {:lifecycle/task :scanner-command-result
            :lifecycle/calls :com.interrupt.streaming.platform.scanner-command/out-calls-scanner-command-result}
           {:lifecycle/task :scanner-command-result
            :lifecycle/calls :onyx.plugin.core-async/writer-calls}

           {:lifecycle/task :scanner
            :lifecycle/calls :com.interrupt.streaming.platform.scanner-command/out-calls-scanner}
           {:lifecycle/task :scanner
            :lifecycle/calls :onyx.plugin.core-async/writer-calls}]}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url platform-type]
  {:input-scanner-command
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
            :kafka/topic "foo"
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


(defn catalog [zookeeper-url platform-type]

  [(merge input-scanner-command
          (-> (catalog-configs zookeeper-url platform-type)
              :input-scanner-command
              platform-type
              (assoc :kafka/topic "scanner-command")))

   function-ibgateway

   (merge output-scanner-command-result
          (-> (catalog-configs zookeeper-url platform-type)
              :output-scanner-command-result
              platform-type
              (assoc :kafka/topic "scanner-command-result")))

   (merge output-scanner
          (-> (catalog-configs zookeeper-url platform-type)
              :output-scanner
              platform-type
              (assoc :kafka/topic "scanner")))])

(comment

  (>!! @base/chan-scanner-command {:foo :bar})
  (def r1 (<!! @base/chan-scanner-command-result))
  (def r2 (<!! @base/chan-scanner))

  #_(>!! @chan-scanner-command {:foo :bar})
  #_(def result (<!! @chan-scanner-command-result)))
