(ns com.interrupt.ewrapper
  (:import [com.ib.client EWrapper EClient EClientSocket EReader EReaderSignal]))


(def clientSocketObj (atom nil))

(defn ewrapper []
  (reify
    EWrapper
    (connectionClosed [_]
      (println "connectionClosed CALLED"))

    #_(connectAck [_]
      (println "connectAck CALLED")

      (when (.isAsyncEConnect clientSocketObj)
        (println "connectAck / Acknowledging connection")
        (.startAPI clientSocketObj)))

    (error [_ id errorCode errorMsg]
      (println (str "Error. Id: "  id  ", Code: "  errorCode  ", Msg: "  errorMsg  "\n")))))


#_(defn ereader-signal []
  (reify
    EReaderSignal
    (issueSignal [_]
      (println "issueSignal CALLED"))
    (waitForSignal [_]
      #_(println "waitForSignal CALLED"))))


#_(defn eclient-socket [wrapper reader-signal]
  (EClientSocket. wrapper reader-signal))


#_(comment

  (def wrapper (ewrapper))
  (def reader-signal (ereader-signal))

  ;; 1. create client socket
  (def clientSocket (eclient-socket wrapper reader-signal))
  (swap! clientSocketObj identity clientSocket)
  (def twsPort 4001)
  (def twsClientId 1)

  (.eConnect clientSocket "edgarly_tws_1" twsPort twsClientId)

  ;; 2. start API is called in `connectAck`

  ;; 3. Start consuming TWS data
  (def eclient (Eclient. wrapper))
  (def ereader (EReader. clientSocket reader-signal)))

(comment

  (import '[com.interrupt.ibgateway EWrapperImpl]
          '[com.ib.client EClientSocket EReaderSignal EReader])

  (def ewrapperImpl (EWrapperImpl.))
  (def client (.getClient ewrapperImpl))
  (def signal (.getSignal ewrapperImpl))

  (.eConnect client "edgarly_tws_1" 4001 0)

  (def ereader (EReader. client signal))
  (.start ereader)

  (def result
    (future
      (while (.isConnected client)
        (.waitForSignal signal)
        (try
          (.processMsgs ereader)
          (catch Exception e
            (println (str "Exception: " (.getMessage e))))))))

  )
