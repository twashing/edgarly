(ns com.interrupt.ewrapper
  (:import [com.ib.client EWrapper EClientSocket EReaderSignal]))

(defn ewrapper []
  (reify
    EWrapper
    (connectionClosed [_]
      (println "connectionClosed CALLED"))

    (connectAck [_]
      (println "connectAck CALLED"))

    (error [_ id errorCode errorMsg]
      (println (str "Error. Id: "  id  ", Code: "  errorCode  ", Msg: "  errorMsg  "\n")))))

(defn ereader-signal []
  (reify
    EReaderSignal
    (issueSignal [_]
      (println "issueSignal CALLED"))
    (waitForSignal [_]
      #_(println "waitForSignal CALLED"))))

(comment

  (def wrapper (ewrapper))
  (def reader-signal (ereader-signal))
  (def clientSocket (EClientSocket. wrapper reader-signal))

  (def twsPort 4001)
  (def twsClientId 1)

  (.eConnect clientSocket "edgarly_tws_1" twsPort twsClientId))
