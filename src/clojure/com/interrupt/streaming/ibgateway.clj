(ns com.interrupt.streaming.ibgateway
  (:require [clojure.core.async :refer [chan pub >!! <!! >! <!]]
            [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]

            [system.repl]))


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
            :lifecycle/calls :com.interrupt.streaming.ibgateway/in-calls-scanner-command}
           {:lifecycle/task :scanner-command
            :lifecycle/calls :onyx.plugin.core-async/reader-calls}

           {:lifecycle/task :scanner-command-result
            :lifecycle/calls :com.interrupt.streaming.ibgateway/out-calls-scanner-command-result}
           {:lifecycle/task :scanner-command-result
            :lifecycle/calls :onyx.plugin.core-async/writer-calls}

           {:lifecycle/task :scanner
            :lifecycle/calls :com.interrupt.streaming.ibgateway/out-calls-scanner}
           {:lifecycle/task :scanner
            :lifecycle/calls :onyx.plugin.core-async/writer-calls}]}
   platform-type))


;; WINDOWS
(def windows
  [{:window/id :collect-segments
    :window/task :ibgateway
    :window/type :global
    :window/aggregation ::scan-aggregation}])

(def triggers
  [{:trigger/window-id :collect-segments
    :trigger/id :collect-segments-trigger
    :trigger/on :onyx.triggers/segment
    :trigger/threshold [1 :elements]
    :trigger/sync ::dump-window!}])


(def thing (atom []))

(defn dump-window! [event window trigger {:keys [lower-bound upper-bound] :as window-data} state]
  (swap! thing conj (format "event[ %s ] / window[ %s ] / trigger[ %s ] / Window extent [%s - %s] / state[ %s ]"
                            event window trigger lower-bound upper-bound state)))


;; AGGREGATION & STATE
(def config
  {:stocks {:default-instrument "STK"
            :default-location "STK.US.MAJOR"}

   :scanners [{:scan-name "HIGH_OPT_IMP_VOLAT"
               :scan-value {}
               :tag :volatility}
              {:scan-name "HIGH_OPT_IMP_VOLAT_OVER_HIST"
               :scan-value {}
               :tag :volatility}
              {:scan-name "HOT_BY_VOLUME"
               :scan-value {}
               :tag :volume}
              {:scan-name "TOP_VOLUME_RATE"
               :scan-value {}
               :tag :volume}
              {:scan-name "HOT_BY_OPT_VOLUME"
               :scan-value {}
               :tag :volume}
              {:scan-name "OPT_VOLUME_MOST_ACTIVE"
               :scan-value {}
               :tag :volume}
              {:scan-name "COMBO_MOST_ACTIVE"
               :scan-value {}
               :tag :volume}
              {:scan-name "MOST_ACTIVE_USD"
               :scan-value {}
               :tag :price}
              {:scan-name "HOT_BY_PRICE"
               :scan-value {}
               :tag :price}
              {:scan-name "TOP_PRICE_RANGE"
               :scan-value {}
               :tag :price}
              {:scan-name "HOT_BY_PRICE_RANGE"
               :scan-value {}
               :tag :price}]})

(defn scan-init-fn [window]

  #_(def client (-> system.repl/system :ewrapper :ewrapper :client))
  #_(def publisher (-> system.repl/system :ewrapper :ewrapper :publisher))

  #_(def publication (pub publisher #(:req-id %)))
  #_(def scanner-subscriptions (tws/scanner-start client publication config))

  0)

(defn scan-aggregation-fn [window segment]

  (println (format "scan-aggregation-fn / window[ %s ] / segment[ %s ]" window segment))
  (let [k (-> segment :message :age)]
    {:value k}))

(defn scan-application-fn [window state value]

  (println (format "scan-application-fn / window[ %s ] state[ %s ] value[ %s ]" window state value))
  (+ state (:value value)))

;; scan-aggregation aggregation referenced in window definition.
(def scan-aggregation
  {:aggregation/init scan-init-fn
   :aggregation/create-state-update scan-aggregation-fn
   :aggregation/apply-state-update scan-application-fn})


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
   :onyx/fn :com.interrupt.streaming.base/local-identity})

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
