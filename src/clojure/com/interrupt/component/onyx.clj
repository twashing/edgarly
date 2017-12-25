(ns com.interrupt.component.onyx
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [onyx.api]

            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]

            [com.interrupt.streaming.platform :as pl]
            [com.interrupt.streaming.platform.scanner-command :as psc]
            [com.interrupt.streaming.platform.scanner :as ps]
            [com.interrupt.streaming.platform.filtered-stocks :as pfs]
            [com.interrupt.streaming.platform.predictive-analytics :as ppa]
            [com.interrupt.streaming.platform.trade-recommendations :as ptr]
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

    (for [[the-workflow the-lifecycles the-catalog] [[psc/workflow
                                                      (psc/lifecycles :kafka)
                                                      (psc/catalog zookeeper-url :kafka)]

                                                     [ps/workflow
                                                      (ps/lifecycles :kafka)
                                                      (ps/catalog zookeeper-url :kafka)]

                                                     [pfs/workflow
                                                      (pfs/lifecycles :kafka)
                                                      (pfs/catalog zookeeper-url :kafka)]

                                                     [ppa/workflow
                                                      (ppa/lifecycles :kafka)
                                                      (ppa/catalog zookeeper-url :kafka)]

                                                     [ptr/workflow
                                                      (ptr/lifecycles :kafka)
                                                      (ptr/catalog zookeeper-url :kafka)]

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
