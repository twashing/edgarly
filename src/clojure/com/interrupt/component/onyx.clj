(ns com.interrupt.component.onyx
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [onyx.api]

            [onyx.plugin.kafka :refer [read-messages write-messages]]

            #_[franzy.clients.producer.protocols :refer :all]
            #_[franzy.clients.consumer.protocols :refer :all]
            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]

            #_[franzy.clients.producer.client :as producer]
            #_[franzy.clients.consumer.client :as consumer]
            #_[franzy.admin.zookeeper.client :as client]
            #_[franzy.admin.topics :as topics]
            #_[franzy.clients.producer.defaults :as pd]
            #_[franzy.clients.consumer.defaults :as cd]

            [com.interrupt.streaming.platform :as pl]
            [com.interrupt.streaming.platform.scanner-command :as psc]
            [com.interrupt.streaming.platform.scanner :as ps]
            [onyx.test-helper :refer [load-config]]
            [aero.core :refer [read-config]]

            [spyscope.core])
  (:import [java.util UUID]))


(defn submit-jobs! [config-file-string]

  (let [{:keys [zookeeper-url] :as config} (read-config (io/resource config-file-string))

        env-config (assoc (:env-config config)
                          :zookeeper/address zookeeper-url)

        peer-config (assoc (:peer-config config)
                           :zookeeper/address zookeeper-url)

        env (onyx.api/start-env env-config)
        peer-group (onyx.api/start-peer-group peer-config)

        peer-count 6
        v-peers (onyx.api/start-peers peer-count peer-group)]

    (for [[the-workflow the-catalog] [[psc/workflow (psc/catalog zookeeper-url "scanner-command")]
                                      #_[ps/workflow (ps/catalog zookeeper-url :scanner :filtered-stocks)]]]

      (let [job {:workflow the-workflow
                 :catalog the-catalog
                 :task-scheduler :onyx.task-scheduler/balanced}
            {:keys [job-id task-ids] :as submitted-job} #_spy/d (onyx.api/submit-job peer-config job)]
        submitted-job))))

(comment

  (def topics (pl/one-setup-topics))
  (def result (pl/two-write-to-topic "scanner-command" (str (UUID/randomUUID)) {:foo :bar}))

  (let [submitted-jobs (submit-jobs! "config.edn")]
    (println ";; Onyx submitted jobs: " submitted-jobs)))


(defrecord Onyx []
  component/Lifecycle

  (start [component]

    (println ";; Starting Onyx")
    #_(pl/one-setup-topics)
    #_(pl/two-write-to-topic "scanner-command" (str (UUID/randomUUID)) {:foo :bar})

    (let [submitted-jobs (submit-jobs! "config.edn")]
      (println ";; Onyx submitted jobs: " submitted-jobs)
      (assoc component :onyx-jobs submitted-jobs)))

  (stop [component]

    (println ";; Stopping Onyx")

    (let [submitted-jobs (:onyx-jobs component)
          {:keys [zookeeper-url] :as config} (read-config (io/resource "config.edn"))
          peer-config (assoc (:peer-config config)
                             :zookeeper/address zookeeper-url)]

      (for [{:keys [job-id task-ids] :as submitted-job} submitted-jobs]
        (onyx.api/kill-job peer-config job-id))

      (dissoc component :onyx))))

(defn new-onyx []
  (map->Onyx {}))
