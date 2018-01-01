(ns com.interrupt.edgarly.core
  (:require  [com.stuartsierra.component :as component]
             [system.repl :refer [set-init! init start stop reset refresh system]]

             [com.interrupt.component.repl-server :refer [new-repl-server]]
             #_[system.components.repl-server :refer [new-repl-server]]
             [com.interrupt.component.ewrapper :refer [new-ewrapper]]
             [com.interrupt.component.onyx :refer [new-onyx]]
             [com.interrupt.component.ewrapper-impl :as ei]
             [clojure.core.async :refer [chan >! <! merge go go-loop pub sub unsub-all sliding-buffer]]

             [com.rpl.specter :refer [transform select ALL]]
             [clojure.math.combinatorics :as cmb]
             [clojure.pprint :refer [pprint]])
  (:import [java.util.concurrent TimeUnit]
           [java.util Calendar]
           [java.text SimpleDateFormat]
           [com.ib.client
            EWrapper EClient EClientSocket EReader EReaderSignal
            Contract ContractDetails ScannerSubscription]
           [com.ib.client Types$BarSize Types$DurationUnit Types$WhatToShow]))


(defn system-map []
  (component/system-map
   :nrepl (new-repl-server 5554 "0.0.0.0")
   ;; :ewrapper (new-ewrapper)
   ;; :onyx (new-onyx)
   ))

(set-init! #'system-map)
(defn start-system [] (start))
(defn stop-system [] (stop))


(defn consume-subscriber-historical [historical-atom subscriber-chan]
  (go-loop [r1 nil]

    (let [{:keys [req-id date open high low close volume count wap has-gaps] :as val} r1]
      (swap! historical-atom assoc date val))
    (recur (<! subscriber-chan))))

(defn historical-start [req-id client publication historical-atom]

  (let [subscriber (chan)]
    (ei/historical-subscribe req-id client)
    (sub publication req-id subscriber)
    (consume-subscriber-historical historical-atom subscriber)))

(defn historical-stop [])

(comment

  ;; TODO

  ;; Add these to the 'platform/ibgateway' namespace
  ;;   scanner-start ( ei/scanner-subscribe )
  ;;   scanner-stop ( ei/scanner-unsubscribe )

  ;; record connection IDs

  ;; migrate data sink atoms (high-opt-imp-volat, high-opt-imp-volat-over-hist, etc)
  ;;   > to core.async channels > then to onyx output (mostly kafka)

  ;; CONFIG for
  ;;   network name of tws

  ;; TESTs for ibgateway
  ;;   enable core.async onyx transport for services
  ;;   workbench for data transport in and out of service
  ;;   workbench for subscribing to tws
  ;;
  ;;   test if open, remain open
  ;;   test if closed, remain closed
  ;;   test start scanning; we capture distinct categories (volatility, etc)
  ;;   test stop scanning
  ;;   test toggle scan
  {:scanner-command :start}
  {:scanner-command :stop}


  ;; write (Transit) to Kafka
  ;; read (Transit) from Kafka
  ;; feed to analysis

  (def client (-> system.repl/system :ewrapper :ewrapper :client))
  (def publisher (-> system.repl/system :ewrapper :ewrapper :publisher))
  (def publication
    (pub publisher #(:req-id %)))

  (def scanner-subscriptions (scanner-start client publication config))
  (pprint scanner-subscriptions)


  ;; HISTORICALDATA
  (def historical-atom (atom {}))
  (def historical-subscriptions (historical-start 4002 client publication historical-atom))

  ;; ====
  ;; Requesting historical data

  ;; (def cal (Calendar/getInstance))
  ;; #_(.add cal Calendar/MONTH -6)
  ;;
  ;; (def form (SimpleDateFormat. "yyyyMMdd HH:mm:ss"))
  ;; (def formatted (.format form (.getTime cal)))
  ;;
  ;; (let [contract (doto (Contract.)
  ;;                  (.symbol "TSLA")
  ;;                  (.secType "STK")
  ;;                  (.currency "USD")
  ;;                  (.exchange "SMART")
  ;;                  #_(.primaryExch "ISLAND"))]
  ;;
  ;;   (.reqHistoricalData client 4002 contract formatted "4 W" "1 min" "MIDPOINT" 1 1 nil))


  (require '[clojure.string :as str])

  (pprint (take 6 (->> @historical-atom
                       (sort-by first)
                       (remove (fn [[k {:keys [date] :as v}]]
                                 (or (nil? k)
                                     (str/starts-with? date "finished-")))))))

  (def historical-final (->> @historical-atom
                             (sort-by first)
                             (remove (fn [[k {:keys [date] :as v}]]
                                       (or (nil? k)
                                           (str/starts-with? date "finished-"))))))


  (pprint (take 7 historical-final))

  ;; 1. write to edn
  #_(spit "tesla-historical-20170901-20170915.edn" @historical-atom)
  (spit "tesla-historical-20170601-20170928.edn" (pr-str historical-final))

  ;; 2. write to json
  (require '[clojure.data.json :as json])
  (spit "tesla-historical-20170601-20170928.json" (json/write-str historical-final))


  (pprint high-opt-imp-volat)
  (pprint high-opt-imp-volat-over-hist)
  (pprint hot-by-volume)
  (pprint top-volume-rate)
  (pprint hot-by-opt-volume)
  (pprint opt-volume-most-active)
  (pprint combo-most-active)
  (pprint most-active-usd)
  (pprint hot-by-price)
  (pprint top-price-range)
  (pprint hot-by-price-range)

  (ei/scanner-unsubscribe 1 client)
  (ei/scanner-unsubscribe 2 client)
  (ei/scanner-unsubscribe 3 client)
  (ei/scanner-unsubscribe 4 client)
  (ei/scanner-unsubscribe 5 client)
  (ei/scanner-unsubscribe 6 client)
  (ei/scanner-unsubscribe 7 client)
  (ei/scanner-unsubscribe 8 client)
  (ei/scanner-unsubscribe 9 client)
  (ei/scanner-unsubscribe 10 client)
  (ei/scanner-unsubscribe 11 client)

  (def ss (let [scan-names (->> config :scanners (map :scan-name))
                scan-subsets #_spy/d (map (fn [sname]
                                           (->> @scanner-subscriptions
                                                (filter (fn [e] (= (::scan-name e) sname)))
                                                first ::scan-value vals (map :symbol)
                                                (fn [e] {sname e}))))]
            scan-subsets))


  (def sone (set (map :symbol (vals @high-opt-imp-volat))))
  (def stwo (set (map :symbol (vals @high-opt-imp-volat-over-hist))))
  (def s-volatility (cs/intersection sone stwo))  ;; OK

  (def sthree (set (map :symbol (vals @hot-by-volume))))
  (def sfour (set (map :symbol (vals @top-volume-rate))))
  (def sfive (set (map :symbol (vals @hot-by-opt-volume))))
  (def ssix (set (map :symbol (vals @opt-volume-most-active))))
  (def sseven (set (map :symbol (vals @combo-most-active))))
  (def s-volume (cs/intersection sthree sfour #_sfive #_ssix #_sseven))

  (def seight (set (map :symbol (vals @most-active-usd))))
  (def snine (set (map :symbol (vals @hot-by-price))))
  (def sten (set (map :symbol (vals @top-price-range))))
  (def seleven (set (map :symbol (vals @hot-by-price-range))))
  (def s-price-change (cs/intersection seight snine #_sten #_seleven))

  (cs/intersection sone stwo snine)
  (cs/intersection sone stwo seleven)


  (def intersection-subsets
    (filter (fn [e] (> (count e) 1))
            (cmb/subsets [{:name "one" :val sone}
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
                    (let [result (apply cs/intersection (map :val e))
                          names (map :name e)]
                      {:names names :intersection result}))
                  intersection-subsets)))

  (def or-volatility-volume-price-change

    (->> (filter (fn [e]
                   (and (> (count (:intersection e)) 1)
                        (some #{"one" "two" "three" "four" "five" "six" "seven" "eight" "nine" "ten" "eleven"} (:names e))
                        #_(or (some #{"one" "two"} (:names e))
                              (some #{"three" "four" "five" "six" "seven"} (:names e))
                              (some #{"eight" "nine" "ten" "eleven"} (:names e)))))
                 sorted-intersections)

         (sort-by #(count (:names %)))))

  (clojure.pprint/pprint intersection-subsets)
  (clojure.pprint/pprint sorted-intersections)
  (clojure.pprint/pprint or-volatility-volume-price-change))

(defn market-start [])

(defn market-stop [])

(defn open-request-ids [])

(defn -main [& args]
  (Thread/sleep 5000) ;; a hack, to ensure that the tws machine is available, before we try to connect to it.
  (start-system))

(comment
  (start-system)
  (reset)
  (stop))
