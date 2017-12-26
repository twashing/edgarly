(ns com.interrupt.component.onyx
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [onyx.api]

            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]
            [com.interrupt.streaming.platform :as pl]
            [com.interrupt.streaming.platform.ibgateway :as pib]
            [com.interrupt.streaming.platform.market-scanner :as pms]
            [com.interrupt.streaming.platform.analytics :as pa]
            [com.interrupt.streaming.platform.clnn :as pcln]
            [com.interrupt.streaming.platform.execution-engine :as pee]
            [com.interrupt.streaming.platform.bookeeping :as pbk]
            [com.interrupt.streaming.platform.edgarly :as ped]
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

    (for [[the-workflow the-lifecycles the-catalog] [[pib/workflow
                                                      (pib/lifecycles :kafka)
                                                      (pib/catalog zookeeper-url :kafka)]

                                                     [pms/workflow
                                                      (pms/lifecycles :kafka)
                                                      (pms/catalog zookeeper-url :kafka)]

                                                     [pa/workflow
                                                      (pa/lifecycles :kafka)
                                                      (pa/catalog zookeeper-url :kafka)]

                                                     [pcln/workflow
                                                      (pcln/lifecycles :kafka)
                                                      (pcln/catalog zookeeper-url :kafka)]

                                                     [pee/workflow
                                                      (pee/lifecycles :kafka)
                                                      (pee/catalog zookeeper-url :kafka)]

                                                     [pbk/workflow
                                                      (pbk/lifecycles :kafka)
                                                      (pbk/catalog zookeeper-url :kafka)]

                                                     [ped/workflow
                                                      (ped/lifecycles :kafka)
                                                      (ped/catalog zookeeper-url :kafka)]]

          :let [peer-count (->> the-catalog (map :onyx/max-peers) (apply +))]]

      (do (println "the-catalog: " the-catalog)
          (onyx.api/start-peers peer-count peer-group)
          (let [job {:workflow the-workflow
                     :catalog the-catalog
                     :lifecycles the-lifecycles
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
