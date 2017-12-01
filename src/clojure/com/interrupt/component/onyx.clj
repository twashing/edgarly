(ns com.interrupt.component.onyx
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [onyx.api]

            [onyx.plugin.kafka :refer [read-messages write-messages]]
            [onyx.kafka.helpers :as h]

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
        ;; _ (println "Sanity 1: " config)

        env-config (assoc (:env-config config)
                          :zookeeper/address zookeeper-url)
        ;; _ (println "Sanity 2: " env-config)

        peer-config (assoc (:peer-config config)
                           :zookeeper/address zookeeper-url)
        ;; _ (println "Sanity 3: " peer-config)

        env (onyx.api/start-env env-config)
        ;; _ (println "Sanity 4: " env)

        peer-group (onyx.api/start-peer-group peer-config)
        ;; _ (println "Sanity 5: " peer-group)

        peer-count 6
        v-peers (onyx.api/start-peers peer-count peer-group)
        ;; _ (println "Sanity 6: " v-peers)
        ]

    (println "Sanity 7: " (psc/catalog zookeeper-url "scanner-command" :onyx))

    (for [[the-workflow the-lifecycles the-catalog] [[psc/workflow
                                                      (psc/lifecycles :kafka)
                                                      (psc/catalog zookeeper-url "scanner-command" :kafka)]

                                                     #_[ps/workflow (ps/catalog zookeeper-url :scanner :filtered-stocks)]]]

      (do (println "the-catalog: " the-catalog)
          (let [job {:workflow the-workflow
                     :catalog the-catalog
                     :lifecycles the-lifecycles
                     :task-scheduler :onyx.task-scheduler/balanced}
                {:keys [job-id task-ids] :as submitted-job} #_spy/d (onyx.api/submit-job peer-config job)]

            submitted-job)))))

#_(comment

  (def topics (pl/one-setup-topics))
  (def result (pl/two-write-to-topic "scanner-command" (str (UUID/randomUUID)) {:foo :bar}))

  (let [submitted-jobs (submit-jobs! "config.edn")]
    (println ";; Onyx submitted jobs: " submitted-jobs)))


(defrecord Onyx []
  component/Lifecycle

  (start [component]

    (println ";; Starting Onyx")
    ;; (submit-jobs! "config.edn")
    ;; (assoc component :onyx-jobs :foo)

    #_(doseq [topic [pl/topic-scanner-command
                   pl/topic-scanner
                   pl/topic-filtered-stocks
                   pl/topic-stock-command]]

      (h/create-topic! "zookeeper:2181" topic 1 1))


    (let [submitted-jobs (submit-jobs! "config.edn")]
      (println ";; Onyx submitted jobs: " submitted-jobs)
      (assoc component :onyx-jobs submitted-jobs)))

  (stop [component]

    (println ";; Stopping Onyx")
    #_(dissoc component :onyx-jobs)

    (let [submitted-jobs (:onyx-jobs component)
          {:keys [zookeeper-url] :as config} (read-config (io/resource "config.edn"))
          peer-config (assoc (:peer-config config)
                             :zookeeper/address zookeeper-url)]

      (doseq [{:keys [job-id task-ids] :as submitted-job} submitted-jobs]
        (onyx.api/kill-job peer-config job-id))

      (dissoc component :onyx-jobs))))

(defn new-onyx []
  (map->Onyx {}))
