(ns com.interrupt.component.onyx_test
  (:require [clojure.test :refer :all]
            [aero.core :refer [read-config]]
            [embedded-kafka.core :refer [with-test-broker] :as ek]
            [com.interrupt.component.onyx :as sut]
            [com.interrupt.streaming.admin :as adm]
            [com.interrupt.streaming.platform.scanner-command :as psc]))


(deftest test-setup-onyx-component
  (with-test-broker producer consumer

    (let [{:keys [zookeeper-url] :as config} (read-config "config.edn" {:profile :test})

          _ (adm/setup-topics
             {:zookeeper-url zookeeper-url}
             (for [topic (concat psc/input-topics psc/output-topics)]
               {:topic topic :partition-count 10}))]

      (is (sut/new-onyx)))))


#_(comment

  (with-test-broker producer consumer

    (let [{:keys [zookeeper-url] :as config} (read-config "config.edn" {:profile :test})

          _ (adm/setup-topics
             {:zookeeper-url zookeeper-url}
             (for [topic (concat psc/input-topics psc/output-topics)]
               {:topic topic :partition-count 10}))]

      (sut/new-onyx))))
