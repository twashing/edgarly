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

(def SCANNERDATA :scanner-data)

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

(defn ewrapper

  ([] (ewrapper 1))
  ([no-of-topics]

   ;; ====
   ;; Setup client, wrapper, process messages
   (let [buffer-size (* no-of-topics (+ 1 50))
         publisher (chan (sliding-buffer 100))
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
      :publisher publisher})))

(comment

  (def one (ewrapper 11))
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


  ;; Sanity 1:  {:topic :scanner-data, :req-id 1, :message-end false, :symbol AGFS, :sec-type #object[com.ib.client.Types$SecType 0x1ff4ec3a STK], :rank 0}
  ;; Sanity 1:  {:topic :scanner-data, :req-id 1, :message-end false, :symbol BAA, :sec-type #object[com.ib.client.Types$SecType 0x1ff4ec3a STK], :rank 1}
  ;; Sanity 1:  {:topic :scanner-data, :req-id 1, :message-end false, :symbol EBR B, :sec-type #object[com.ib.client.Types$SecType 0x1ff4ec3a STK], :rank 2}


  (require '[system.repl])
  (def client (-> system.repl/system :ewrapper :ewrapper :client))
  (def publisher (-> system.repl/system :ewrapper :ewrapper :publisher))

  ;; ===
  ;; Stock scanners
  (def default-instrument "STK")
  (def default-location "STK.US.MAJOR")

  ;; Volatility
  (scanner-subscribe 1 client default-instrument default-location "HIGH_OPT_IMP_VOLAT")
  (scanner-subscribe 2 client default-instrument default-location "HIGH_OPT_IMP_VOLAT_OVER_HIST")

  ;; Volume
  (scanner-subscribe 3 client default-instrument default-location "HOT_BY_VOLUME")
  (scanner-subscribe 4 client default-instrument default-location "TOP_VOLUME_RATE")
  (scanner-subscribe 5 client default-instrument default-location "HOT_BY_OPT_VOLUME")
  (scanner-subscribe 6 client default-instrument default-location "OPT_VOLUME_MOST_ACTIVE")
  (scanner-subscribe 7 client default-instrument default-location "COMBO_MOST_ACTIVE")

  ;; Price Change
  (scanner-subscribe 8 client default-instrument default-location "MOST_ACTIVE_USD")
  (scanner-subscribe 9 client default-instrument default-location "HOT_BY_PRICE")
  (scanner-subscribe 10 client default-instrument default-location "TOP_PRICE_RANGE")
  (scanner-subscribe 11 client default-instrument default-location "HOT_BY_PRICE_RANGE")


  ;; Subscribe
  (def publication
    (pub publisher #(:req-id %)))

  (def subscriber-one (chan))
  (def subscriber-two (chan))
  (def subscriber-three (chan))
  (def subscriber-four (chan))
  (def subscriber-five (chan))
  (def subscriber-six (chan))
  (def subscriber-seven (chan))
  (def subscriber-eight (chan))
  (def subscriber-nine (chan))
  (def subscriber-ten (chan))
  (def subscriber-eleven (chan))

  (sub publication 1 subscriber-one)
  (sub publication 2 subscriber-two)
  (sub publication 3 subscriber-three)
  (sub publication 4 subscriber-four)
  (sub publication 5 subscriber-five)
  (sub publication 6 subscriber-six)
  (sub publication 7 subscriber-seven)
  (sub publication 8 subscriber-eight)
  (sub publication 9 subscriber-nine)
  (sub publication 10 subscriber-ten)
  (sub publication 11 subscriber-eleven)

  (defn consume-subscriber [dest-atom subscriber]
    (go-loop [r1 nil]

      (let [{:keys [req-id symbol rank] :as val} (select-keys r1 [:req-id :symbol :rank])]
        (if (and r1 rank)
          (swap! dest-atom assoc rank val)))

      (recur (<! subscriber))))

  ;; Buckets
  (def volat-one (atom {}))
  (def volat-two (atom {}))
  (def volat-three (atom {}))
  (def volat-four (atom {}))
  (def volat-five (atom {}))
  (def volat-six (atom {}))
  (def volat-seven (atom {}))
  (def volat-eight (atom {}))
  (def volat-nine (atom {}))
  (def volat-ten (atom {}))
  (def volat-eleven (atom {}))

  (consume-subscriber volat-one subscriber-one)
  (consume-subscriber volat-two subscriber-two)
  (consume-subscriber volat-three subscriber-three)
  (consume-subscriber volat-four subscriber-four)
  (consume-subscriber volat-five subscriber-five)
  (consume-subscriber volat-six subscriber-six)
  (consume-subscriber volat-seven subscriber-seven)
  (consume-subscriber volat-eight subscriber-eight)
  (consume-subscriber volat-nine subscriber-nine)
  (consume-subscriber volat-ten subscriber-ten)
  (consume-subscriber volat-eleven subscriber-eleven)

  ;; Intersection
  (require '[clojure.set :as s])
  (def sone (set (map :symbol (vals @volat-one))))
  (def stwo (set (map :symbol (vals @volat-two))))
  (def s-volatility (s/intersection sone stwo))  ;; OK

  (def sthree (set (map :symbol (vals @volat-three))))
  (def sfour (set (map :symbol (vals @volat-four))))
  (def sfive (set (map :symbol (vals @volat-five))))
  (def ssix (set (map :symbol (vals @volat-six))))
  (def sseven (set (map :symbol (vals @volat-seven))))
  (def s-volume (s/intersection sthree sfour #_sfive #_ssix #_sseven))

  (def seight (set (map :symbol (vals @volat-eight))))
  (def snine (set (map :symbol (vals @volat-nine))))
  (def sten (set (map :symbol (vals @volat-ten))))
  (def seleven (set (map :symbol (vals @volat-eleven))))
  (def s-price-change (s/intersection #_seight #_snine sten seleven))

  (s/intersection sone stwo snine)
  (s/intersection sone stwo seleven)


  (require '[clojure.math.combinatorics :as combo])
  (def intersection-subsets
    (filter (fn [e] (> (count e) 1))
            (combo/subsets [{:name "one" :val sone}
                            {:name "two" :val stwo}
                            {:name "three" :val sthree}
                            {:name "four" :val sfour}
                            {:name "five" :val sfive}
                            {:name "six" :val ssix}
                            {:name "seven" :val sseven}
                            {:name "eight" :val seight}
                            {:name "nine" :val snine}
                            {:name "ten" :val sten}
                            {:name "eleven" :val seleven}])))

  (def sorted-intersections
    (sort-by #(count (:intersection %))
             (map (fn [e]
                    (let [result (apply s/intersection (map :val e))
                          names (map :name e)]
                      {:names names :intersection result}))
                  intersection-subsets)))

  (def or-volatility-volume-price-change
    (filter (fn [e]
              (and (> (count (:intersection e)) 1)
                   (some #{"one" "two"} (:names e))
                   (some #{"three" "four" "five" "six" "seven"} (:names e))
                   (some #{"eight" "nine" "ten" "eleven"} (:names e))))
            sorted-intersections)


    ;; Unsubscribe
    (scanner-unsubscribe 1 client))
  (scanner-unsubscribe 2 client)
  (scanner-unsubscribe 3 client)
  (scanner-unsubscribe 4 client)
  (scanner-unsubscribe 5 client)
  (scanner-unsubscribe 6 client)
  (scanner-unsubscribe 7 client)
  (scanner-unsubscribe 8 client)
  (scanner-unsubscribe 9 client)
  (scanner-unsubscribe 10 client)
  (scanner-unsubscribe 11 client))
