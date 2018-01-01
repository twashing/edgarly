(ns com.interrupt.streaming.market-scanner
  (:require [clojure.core.async :refer [chan >!! <!! >! <!]]
            [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]))


;; WORKFLOW
(def workflow
  [[:scanner :market-scanner]
   [:market-scanner :filtered-stocks]])

(def input-topics
  [(ffirst workflow)])

(def output-topics
  [(-> workflow last last)])


;; LIFECYCLES
(def in-buffer (atom {}))

(defn inject-scanner-ch [event lifecycle]
  {:core.async/buffer in-buffer
   :core.async/chan @base/chan-scanner})
(defn inject-filtered-stocks-ch [event lifecycle] {:core.async/chan @base/chan-filtered-stocks})

(def in-calls-scanner {:lifecycle/before-task-start inject-scanner-ch})
(def out-calls-filtered-stocks {:lifecycle/before-task-start inject-filtered-stocks-ch})

(defn lifecycles [platform-type]
  ({:kafka []
    :onyx [{:lifecycle/task :scanner
            :lifecycle/calls :com.interrupt.streaming.market-scanner/in-calls-scanner}
           {:lifecycle/task :scanner
            :lifecycle/calls :onyx.plugin.core-async/reader-calls}

           {:lifecycle/task :filtered-stocks
            :lifecycle/calls :com.interrupt.streaming.market-scanner/out-calls-filtered-stocks}
           {:lifecycle/task :filtered-stocks
            :lifecycle/calls :onyx.plugin.core-async/writer-calls}]}
   platform-type))


;; CATALOGS
(defn catalog-configs [zookeeper-url]
  {:input-scanner
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

   :output-filtered-stocks
   {:kafka {:onyx/medium :kafka
            :onyx/plugin :onyx.plugin.kafka/write-messages
            :kafka/zookeeper zookeeper-url
            :kafka/topic "foo"
            :kafka/key-serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-key
            :kafka/serializer-fn :com.interrupt.streaming.platform.serialization/serialize-kafka-message
            :kafka/request-size 307200}

    :onyx {:onyx/medium :core.async
           :onyx/plugin :onyx.plugin.core-async/output}}})

(def input-scanner
  {:onyx/name :scanner
   :onyx/type :input
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Read from the 'scanner' Kafka topic"})

(def function-market-scanner
  {:onyx/name :market-scanner
   :onyx/type :function
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/fn :com.interrupt.streaming.platform.base/local-identity})

(def output-filtered-stocks
  {:onyx/name :filtered-stocks
   :onyx/type :output
   :onyx/min-peers 1
   :onyx/max-peers 1
   :onyx/batch-size 10
   :onyx/doc "Writes messages to the 'filtered-stocks' Kafka topic"})


(defn catalog [zookeeper-url platform-type]
  [(merge input-scanner
          (-> (catalog-configs zookeeper-url)
              :input-scanner
              platform-type
              (assoc :kafka/topic "scanner")))

   function-market-scanner

   (merge output-filtered-stocks
          (-> (catalog-configs zookeeper-url)
              :output-filtered-stocks
              platform-type
              (assoc :kafka/topic "filtered-stocks")))])

(comment

  (>!! @base/chan-scanner-command {:foo :bar})
  (def r1 (<!! @base/chan-scanner-command-result))

  (>!! @base/chan-scanner {:qwerty :asdf})
  (def r2 (<!! @base/chan-filtered-stocks)))
