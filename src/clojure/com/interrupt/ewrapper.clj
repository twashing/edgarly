(ns com.interrupt.ewrapper
  (:import [com.ib.client EWrapper]))

(defn ewrapper []
  (reify
    EWrapper
    (connectionClosed [this])
    (connectAck [this])))

(comment
  (def thing (ewrapper)))
