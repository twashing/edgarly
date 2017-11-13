(ns com.interrupt.component.onyx
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [onyx.api]
            [franzy.clients.producer.protocols :refer :all]
            [franzy.clients.consumer.protocols :refer :all]
            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]
            [com.interrupt.streaming.platform :as pl]
            [com.interrupt.streaming.platform.scanner-command :as psc]
            [com.interrupt.streaming.platform.scanner :as ps]
            [onyx.test-helper :refer [load-config]]
            [aero.core :refer [read-config]]

            [spyscope.core])
  (:import [java.util UUID]))


(defn submit-jobs! [config-file-string]

  (let [{:keys [zookeeper-url] :as config} (read-config (io/resource config-file-string))
        env-config #_spy/d (assoc (:env-config config)
                          :zookeeper/address zookeeper-url)
        peer-config #_spy/d (assoc (:peer-config config)
                           :zookeeper/address zookeeper-url)
        env #_spy/d (onyx.api/start-env env-config)
        peer-group #_spy/d (onyx.api/start-peer-group peer-config)

        peer-count 6 ;; #_#spy/d (n-peers the-catalog the-workflow)
        v-peers #_spy/d (onyx.api/start-peers peer-count peer-group)]

    (for [[the-workflow the-catalog] [[psc/workflow (psc/catalog zookeeper-url "scanner-command" "scanner")]
                                      [ps/workflow (ps/catalog zookeeper-url "scanner" "filtered-stocks")]]]

      (let [job {:workflow the-workflow
                 :catalog the-catalog
                 :task-scheduler :onyx.task-scheduler/balanced}
            {:keys [job-id task-ids] :as submitted-job} #_spy/d (onyx.api/submit-job peer-config job)]
        submitted-job))))

(comment

  (pl/one-setup-topics)
  (pl/two-write-to-topic "scanner-command" (str (UUID/randomUUID)) {:foo :bar})
  (submit-jobs! "config.edn"))


(defrecord Onyx []
  component/Lifecycle

  (start [component]

    (println ";; Starting Onyx")
    ;; (pl/one-setup-topics)
    ;; (pl/two-write-to-topic "scanner-command" (str (UUID/randomUUID)) {:foo :bar})

    (let [submitted-jobs (submit-jobs! "config.edn")]
      (assoc component :onyx-jobs submitted-jobs)))

  (stop [component]

    (println ";; Stopping Onyx")

    (let [submitted-jobs #_spy/d (:onyx-jobs component)
          {:keys [zookeeper-url] :as config} (read-config (io/resource "config.edn"))
          peer-config (assoc (:peer-config config)
                             :zookeeper/address zookeeper-url)]

      (for [{:keys [job-id task-ids] :as submitted-job} submitted-jobs]
        (onyx.api/kill-job peer-config job-id))

      (dissoc component :onyx))))

(defn new-onyx []
  (map->Onyx {}))
