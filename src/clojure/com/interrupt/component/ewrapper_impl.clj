(ns com.interrupt.component.ewrapper-impl
  (:require [clojure.core.async :refer [chan >! <! merge go go-loop pub sub unsub-all sliding-buffer]]
            [clojure.core.match :refer [match]])
  (:import [java.util Calendar]
           [java.text SimpleDateFormat]
           [com.interrupt.ibgateway.contracts ContractSamples]
           [com.interrupt.ibgateway.scanner ScannerSubscriptionSamples]
           [com.interrupt.ibgateway EWrapperImpl]
           [com.ib.client
            EWrapper EClient EClientSocket EReader EReaderSignal
            ContractDetails ScannerSubscription]))


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

  ;; (.eConnect clientSocket "edgarly_tws_1" twsPort twsClientId)

  ;; 2. start API is called in `connectAck`

  ;; 3. Start consuming TWS data
  (def eclient (Eclient. wrapper))
  (def ereader (EReader. clientSocket reader-signal)))

(defn scanner-subscripion [instrument location-code scan-code]
  (doto (ScannerSubscription.)
    (.instrument instrument)
    (.locationCode location-code)
    (.scanCode scan-code)))

(defn scanner-subscribe [req-id client instrument location-code scan-code]

  (let [subscription (scanner-subscripion instrument location-code scan-code)]
    (.reqScannerSubscription client req-id subscription nil)
    req-id))

(defn scanner-unsubscribe [req-id client]
  (.cancelScannerSubscription client req-id))


(def SCANNERDATA :scanner-data)

(defn ewrapper-impl [publisher]

  (proxy [EWrapperImpl] []

    (scannerParameters [^String xml]

      (println "scannerParameters CALLED")
      (def scannerParameters xml))

    (scannerData [reqId rank ^ContractDetails contractDetails ^String distance ^String benchmark ^String projection ^String legsStr]

      (let [sym (.. contractDetails contract symbol)
            sec-type (.. contractDetails contract secType)
            curr (.. contractDetails contract currency)

            ch-value {:topic SCANNERDATA
                      :req-id reqId
                      :message-end false
                      :symbol sym
                      :sec-type sec-type
                      :rank rank}]

        #_(println (str "ScannerData CALLED / reqId: " reqId " - Rank: " rank ", Symbol: " sym
                      ", SecType: " sec-type ", Currency: " curr ", Distance: " distance
                      ", Benchmark: " benchmark ", Projection: " projection ", Legs String: " legsStr))

        (go (>! publisher ch-value))))

    (scannerDataEnd [reqId]

      (let [ch-value {:topic SCANNERDATA
                      :req-id reqId
                      :message-end true}]

        #_(println (str "ScannerDataEnd CALLED / reqId: " reqId))

        (go (>! publisher ch-value))))))


(defn step-one []

  ;; ====
  ;; Setup client, wrapper, process messages

  (let [publisher (chan (sliding-buffer 100))
        ;; broadcast-channel (pub publisher #(:topic %))
        ewrapperImpl (ewrapper-impl publisher)
        client (.getClient ewrapperImpl)
        signal (.getSignal ewrapperImpl)

        result (.eConnect client "edgarly_tws_1" 4002 1)

        ereader (EReader. client signal)]

    ;; (if (.isConnected esocket)
    ;;   (.eDisconnect esocket))

    (.start ereader)
    (future
      (while (.isConnected client)
        (.waitForSignal signal)
        (try
          (.processMsgs ereader)
          (catch Exception e
            (println (str "Exception: " (.getMessage e)))))))

    {:client client
     :publisher publisher}))


(comment

  (def one (step-one))
  (def client (:client one))
  (def publisher (:publisher one))

  ;; ====
  ;; Requesting historical data

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
  (spit "scannerParameters.1.xml" scannerParameters)

  (.reqScannerSubscription client 7001 (ScannerSubscriptionSamples/HighOptVolumePCRatioUSIndexes) nil)

  ;; Canceling the scanner subscription
  (.cancelScannerSubscription client 7001)


  ;; ====
  ;; Core.async workbench
  ;; (def subscriber-one (chan))
  ;; (def subscriber-two (chan))
  ;;
  ;; (sub broadcast-channel SCANNERDATA subscriber-one)
  ;; (sub broadcast-channel SCANNERDATA subscriber-two)
  ;;
  ;; (def high-volatility-one (atom {}))
  ;; (def high-volatility-two (atom {}))
  ;;
  ;;
  ;; ;; ====
  ;; (defn take-and-save [channel topic req-id]
  ;;   (go-loop []
  ;;     (let [msg (<! channel)]
  ;;
  ;;       (if (and (= (:topic msg) topic)
  ;;                (= (:req-id msg) req-id))
  ;;         (swap! high-volatility-one assoc (:symbol msg) msg)
  ;;         (swap! high-volatility-two assoc (:symbol msg) msg))
  ;;       (recur))))
  ;;
  ;; (take-and-save subscriber-one SCANNERDATA 1)
  ;; (take-and-save subscriber-two SCANNERDATA 2)


  ;; (go (>! publisher {:topic SCANNERDATA
  ;;                    :req-id 1
  ;;                    :message-end false
  ;;                    :symbol :foo
  ;;                    :sec-type "stock"}))
  ;;
  ;; (go (>! publisher {:topic SCANNERDATA
  ;;                    :req-id 2
  ;;                    :message-end false
  ;;                    :symbol :bar
  ;;                    :sec-type "stock"}))
  ;;
  ;;
  ;; (unsub-all broadcast-channel SCANNERDATA)
  ;;
  ;; (def scanSub1 (ScannerSubscription.))
  ;; (.instrument scanSub1 "STK")
  ;; (.locationCode scanSub1 "STK.US.MAJOR")
  ;; (.scanCode scanSub1 "HIGH_OPT_IMP_VOLAT")
  ;; (.reqScannerSubscription client 7001 scanSub1 nil)
  ;; (.cancelScannerSubscription client 7001)
  ;;
  ;; (def scanSub2 (ScannerSubscription.))
  ;; (.instrument scanSub2 "STK")
  ;; (.locationCode scanSub2 "STK.US.MAJOR")
  ;; (.scanCode scanSub2 "HIGH_OPT_IMP_VOLAT_OVER_HIST")
  ;; (.reqScannerSubscription client 7002 scanSub2 nil)
  ;; (.cancelScannerSubscription client 7002)


  ;; ===
  ;; Stock scanners
  (def default-instrument "STK")
  (def default-location "STK.US.MAJOR")

  ;; Volatility
  (scanner-subscribe 1 client default-instrument default-location "HIGH_OPT_IMP_VOLAT")
  ;; (scanner-subscribe 2 client default-instrument default-location "HIGH_OPT_IMP_VOLAT_OVER_HIST")

  (go-loop [r1 nil]

    (println "Sanity 1: " r1)
    (select-keys r1 [:symbol :rank])
    (recur (<! publisher)))

  (println "foo")

  (scanner-unsubscribe 1 client)
  (scanner-unsubscribe 2 client)
  ;;
  ;; ;; Volume
  ;; (scanner-subscribe 3 client default-instrument default-location "HOT_BY_VOLUME")
  ;; (scanner-subscribe 4 client default-instrument default-location "TOP_VOLUME_RATE")
  ;; (scanner-subscribe 5 client default-instrument default-location "HOT_BY_OPT_VOLUME")
  ;; (scanner-subscribe 6 client default-instrument default-location "OPT_VOLUME_MOST_ACTIVE")
  ;; (scanner-subscribe 7 client default-instrument default-location "COMBO_MOST_ACTIVE")
  ;;
  ;; ;; Price Change
  ;; (scanner-subscribe 8 client default-instrument default-location "MOST_ACTIVE_USD")
  ;; (scanner-subscribe 9 client default-instrument default-location "HOT_BY_PRICE")
  ;; (scanner-subscribe 10 client default-instrument default-location "TOP_PRICE_RANGE")
  ;; (scanner-subscribe 11 client default-instrument default-location "HOT_BY_PRICE_RANGE")
  )
