(ns com.interrupt.component.ewrapper
  (:require [com.stuartsierra.component :as component]
            [com.interrupt.component.ewrapper-impl :as ewi]))


(defrecord EWrapper []
  component/Lifecycle

  (start [component]

    (println ";; Starting EWrapper")

    ;; Return an updated version of the component with
    ;; the run-time state assoc'd in.
    (let [ewrapper (ewi/ewrapper)]
      (assoc component :ewrapper ewrapper)))

  (stop [component]

    (println ";; Stopping EWrapper")

    ;; In the 'stop' method, shut down the running
    ;; component and release any external resources it has
    ;; acquired.

    ;; Return the component, optionally modified. Remember that if you
    ;; dissoc one of a record's base fields, you get a plain map.
    (let [{:keys [client]} (:ewrapper component)]
      (if (.isConnected client)
        (.eDisconnect client))
      (dissoc component :ewrapper))))

(defn new-ewrapper []
  (map->EWrapper {}))
