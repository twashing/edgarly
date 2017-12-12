(ns com.interrupt.component.service
  (:require [com.stuartsierra.component :as component]

            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]

            [com.interrupt.streaming.platform :as pl]
            [com.interrupt.streaming.platform.scanner-command :as psc]
            [com.interrupt.streaming.platform.scanner :as ps]
            [aero.core :refer [read-config]]))


(defrecord Service []
  component/Lifecycle

  (start [component]

    (println ";; Starting Service")
    (assoc component :service :foo))

  (stop [component]

    (println ";; Stopping Service")
    (dissoc component :service)))

(defn new-service []
  (map->Service {}))
