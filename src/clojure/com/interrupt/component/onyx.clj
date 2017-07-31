(ns com.interrupt.component.onyx
  (:require [com.stuartsierra.component :as component]
            [onyx.api]
            [com.interrupt.streaming.platform.scanner-command :as psc]
            [com.interrupt.streaming.platform.scanner :as ps]
            [onyx.test-helper :refer [load-config]]
            [aero.core :refer [read-config]]))


(defrecord Onyx []
  component/Lifecycle

  (start [component]

    (println ";; Starting Onyx")

    (let [{:keys [zookeeper-url] :as config} (read-config "config.edn")
          env-config (assoc (:env-config config)
                            :zookeeper/address zookeeper-url)
          peer-config (assoc (:peer-config config)
                             :zookeeper/address zookeeper-url)
          env (onyx.api/start-env env-config)
          peer-group (onyx.api/start-peer-group peer-config)

          peer-count 6 ;; #spy/d (n-peers the-catalog the-workflow)
          v-peers (onyx.api/start-peers peer-count peer-group)

          submitted-jobs #spy/d (for [[the-workflow the-catalog] [[psc/workflow (psc/catalog zookeeper-url "scanner-command" "scanner")]
                                                                  [ps/workflow (ps/catalog zookeeper-url "scanner" "filtered-stocks")]]]

                                  (let [job {:workflow the-workflow
                                             :catalog the-catalog
                                             :task-scheduler :onyx.task-scheduler/balanced}
                                        {:keys [job-id task-ids] :as submitted-job} (onyx.api/submit-job peer-config job)]
                                    submitted-job))]

      (assoc component :onyx-jobs submitted-jobs)))

  (stop [component]

    (println ";; Stopping Onyx")

    (let [submitted-jobs #spy/d (:onyx-jobs component)

          {:keys [zookeeper-url] :as config} (read-config "config.edn")
          peer-config (assoc (:peer-config config)
                             :zookeeper/address zookeeper-url)]

      (for [{:keys [job-id task-ids] :as submitted-job} submitted-jobs]
        (onyx.api/kill-job peer-config job-id))

      (dissoc component :onyx))))

(defn new-onyx []
  (map->Onyx {}))
