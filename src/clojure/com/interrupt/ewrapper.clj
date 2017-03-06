(ns com.interrupt.ewrapper
  (:import [com.ib.client EWrapper EClient EClientSocket EReader EReaderSignal]))


(def clientSocketObj (atom nil))

#_(defn ewrapper []
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

(defn ewrapper-impl []
  (proxy [EWrapperImpl] []

    (scannerParameters [^String xml]
      (println "scannerParameters CALLED")
      (def scannerParameters xml))

    ))

(comment

  ;; Setup client, wrapper, process messages
  (import '[com.interrupt.ibgateway EWrapperImpl]
          '[com.ib.client EClientSocket EReaderSignal EReader])

  ;; (def ewrapperImpl (EWrapperImpl.))
  (def ewrapperImpl (ewrapper-impl))
  (def client (.getClient ewrapperImpl))
  (def signal (.getSignal ewrapperImpl))


  (.eConnect client "edgarly_tws_1" 4002 1)

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


  ;; Requesting historical data
  (import '[java.util Calendar]
          '[java.text SimpleDateFormat]
          '[com.ib.client ScannerSubscription]
          '[com.interrupt.ibgateway.contracts ContractSamples]
          '[com.interrupt.ibgateway.scanner ScannerSubscriptionSamples])

  (def cal (Calendar/getInstance))
  (.add cal Calendar/MONTH -6)

  (def form (SimpleDateFormat. "yyyyMMdd HH:mm:ss"))
  (def formatted (.format form (.getTime cal)))

  (.reqHistoricalData client 4001 (ContractSamples/EurGbpFx) formatted "1 M" "1 day" "MIDPOINT" 1 1 nil)
  (.reqHistoricalData client 4002 (ContractSamples/USStockWithPrimaryExch) formatted "1 M" "1 day" "MIDPOINT" 1 1 nil)

  ;; private static void marketScanners(EClientSocket client) throws InterruptedException

  ;; Requesting all available parameters which can be used to build a scanner request
  (.reqScannerParameters client)

  ;; Triggering a scanner subscription
  (spit "scannerParameters.xml" scannerParameters)

  (.reqScannerSubscription client 7001 (ScannerSubscriptionSamples/HighOptVolumePCRatioUSIndexes) nil)

  ;; Canceling the scanner subscription
  (.cancelScannerSubscription client 7001)


  ;; > Volatility
  ;; Highest Option Imp Vol
  ;; HIGH_OPT_IMP_VOLAT
  ;;
  ;; High Option Imp Vol Over Historical
  ;; HIGH_OPT_IMP_VOLAT_OVER_HIST
  ;;
  ;; Top Option Imp Vol % Gainers
  ;; TOP_OPT_IMP_VOLAT_GAIN
  ;;
  ;;
  ;; > Volume
  ;; Hot Contracts by Volume
  ;; HOT_BY_VOLUME
  ;;
  ;; Top Volume Rate
  ;; TOP_VOLUME_RATE
  ;;
  ;; High 3min Volume
  ;; HIGH_STVOLUME_3MIN
  ;;
  ;; High 5min Volume
  ;; HIGH_STVOLUME_5MIN
  ;;
  ;; High 10min Volume
  ;; HIGH_STVOLUME_10MIN
  ;;
  ;; Most Active By Opt Volume
  ;; OPT_VOLUME_MOST_ACTIVE
  ;;
  ;; Most Active By Opt Volume
  ;; OPT_VOLUME_MOST_ACTIVE
  ;;
  ;; High Opt Volume P/C Ratio
  ;; HIGH_OPT_VOLUME_PUT_CALL_RATIO
  ;;
  ;; Hot By Opt Volume
  ;; HOT_BY_OPT_VOLUME
  ;;
  ;; Volume
  ;; COMBO_MOST_ACTIVE
  ;;
  ;; All (Volume Asc)
  ;; COMBO_ALL_VOLUME_ASC
  ;;
  ;; All (Volume Desc)
  ;; COMBO_ALL_VOLUME_DESC
  ;;
  ;;
  ;; > Price Change
  ;; Most Active ($)
  ;; MOST_ACTIVE_USD
  ;;
  ;; Hot Contracts by Price
  ;; HOT_BY_PRICE
  ;;
  ;; Top Price Range
  ;; TOP_PRICE_RANGE
  ;;
  ;; Hot By Price Range
  ;; HOT_BY_PRICE_RANGE
  ;;
  ;; Most Active By Opt Volume
  ;; OPT_VOLUME_MOST_ACTIVE
  ;;
  ;; Most Active By Opt Open Interest
  ;; OPT_OPEN_INTEREST_MOST_ACTIVE

  (def scanSub (ScannerSubscription.))
  (.instrument scanSub "STK")
  (.locationCode scanSub "STK.US.MAJOR")
  (.scanCode scanSub "HIGH_OPT_IMP_VOLAT")

  (.reqScannerSubscription client 7002 scanSub nil)
  (.cancelScannerSubscription client 7002))
