(ns com.interrupt.component.onyx
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [onyx.api]

            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]
            [com.interrupt.streaming.platform :as pl]
            [com.interrupt.streaming.ibgateway :as ib]
            [com.interrupt.streaming.market-scanner :as ms]
            [com.interrupt.streaming.analytics :as an]
            [com.interrupt.streaming.clnn :as cln]
            [com.interrupt.streaming.execution-engine :as ee]
            [com.interrupt.streaming.bookeeping :as bk]
            [com.interrupt.streaming.edgarly :as ed]
            [onyx.test-helper :refer [load-config]]
            [aero.core :refer [read-config]]

            [spyscope.core])
  (:import [java.util UUID]))


(defn submit-jobs! [config-file-string]

  (let [{:keys [zookeeper-url] :as config} (read-config (io/resource config-file-string))
        ;; env-config (assoc (:env-config config) :zookeeper/address zookeeper-url)
        ;; env (onyx.api/start-env env-config)

        peer-config (:peer-config config)
        peer-group (onyx.api/start-peer-group peer-config)]

    (for [[workflow lifecycles catalog
           windows triggers] [[ib/workflow
                               (ib/lifecycles :kafka)
                               (ib/catalog zookeeper-url :kafka)
                               ib/windows
                               ib/triggers]

                              #_[ms/workflow
                                 (ms/lifecycles :kafka)
                                 (ms/catalog zookeeper-url :kafka)]

                              #_[an/workflow
                                 (an/lifecycles :kafka)
                                 (an/catalog zookeeper-url :kafka)]

                              #_[cln/workflow
                                 (cln/lifecycles :kafka)
                                 (cln/catalog zookeeper-url :kafka)]

                              #_[ee/workflow
                                 (ee/lifecycles :kafka)
                                 (ee/catalog zookeeper-url :kafka)]

                              #_[bk/workflow
                                 (bk/lifecycles :kafka)
                                 (bk/catalog zookeeper-url :kafka)]

                              #_[ed/workflow
                                 (ed/lifecycles :kafka)
                                 (ed/catalog zookeeper-url :kafka)]]

          :let [peer-count (->> catalog (map :onyx/max-peers) (apply +))]]

      (do (println "catalog: " catalog)
          (onyx.api/start-peers peer-count peer-group)
          (let [job {:workflow workflow
                     :catalog catalog
                     :lifecycles lifecycles
                     :windows windows
                     :triggers triggers
                     :task-scheduler :onyx.task-scheduler/balanced}
                {:keys [job-id task-ids] :as submitted-job} (onyx.api/submit-job peer-config job)]

            submitted-job)))))

(comment

  (onyx.api/job-state "zookeeper:2181" "dev" "0d4c49db-aba8-b94e-de0b-315e11373feb"))

(defrecord Onyx []
  component/Lifecycle

  (start [component]
    (println ";; Starting Onyx")
    (let [submitted-jobs (submit-jobs! "config.edn")]
      (println ";; Onyx submitted jobs: " submitted-jobs)
      (assoc component :onyx-jobs submitted-jobs)))

  (stop [component]

    (println ";; Stopping Onyx")

    (let [submitted-jobs (:onyx-jobs component)
          {:keys [zookeeper-url] :as config} (read-config (io/resource "config.edn"))
          peer-config (assoc (:peer-config config)
                             :zookeeper/address zookeeper-url)]

      (doseq [{:keys [job-id task-ids] :as submitted-job} submitted-jobs]
        (onyx.api/kill-job peer-config job-id))

      (dissoc component :onyx-jobs))))

(defn new-onyx []
  (map->Onyx {}))
