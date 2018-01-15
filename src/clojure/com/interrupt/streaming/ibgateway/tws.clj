(ns com.interrupt.streaming.ibgateway.tws
  (:require [clojure.spec.alpha :as s]
            [clojure.future :refer [pos-int?]]
            [clojure.core.async :refer [chan pub sub go-loop >!! <!! >! <!]]
            [clojure.set :as cs]
            [clojure.string :as str]
            [clojure.core.match :refer [match]]
            [system.repl]

            [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]
            [com.interrupt.component.ewrapper-impl :as ei]))

(s/def ::reqid pos-int?)
(s/def ::subscription-element (s/keys :req [::reqid]))
(s/def ::subscriptions (s/coll-of ::subscription-element))

(defn scannerid-availableid-pairs [scanner-subscriptions]
  (let [scannerids (sort (map ::reqid scanner-subscriptions))
        scannerids-largest (last scannerids)
        first-id (first scannerids)
        contiguous-numbers (take 10 (range 1 scannerids-largest))
        availableids (sort (cs/difference (into #{} contiguous-numbers)
                                          (into #{} scannerids)))]

    [scannerids availableids]))

(defn next-reqid [scanner-subscriptions]
  (match [scanner-subscriptions]
         [nil] 1
         [[]] 1
         :else (let [[scannerids availableids] (scannerid-availableid-pairs scanner-subscriptions)]
                 (if-not (empty? availableids)
                   (first availableids)
                   (+ 1 (last scannerids))))))

(s/fdef next-reqid
        :args (s/cat :subscriptions ::subscriptions)
        :ret number?
        :fn (s/and

             ;; Handles nil and empty sets
             #(if (empty? (-> % :args :subscriptions))
                (= 1 (:ret %))
                (pos-int? (:ret %)))

             ;; Finds the first gap number
             ;; Can be in first position
             ;; Gap can be on left or right side
             (fn [x]
               (let [reqids (sort (map ::reqid (-> x :args :subscriptions)))
                     fid (first reqids)]
                 (match [fid]
                        [nil] 1
                        [(_ :guard #(> % 1))] (= 1 (:ret x))
                        :else (pos-int? (:ret x)))))))

(defn scan-key [scan-name]
  (-> scan-name (str/lower-case) (str/replace "_" "-") keyword))

(defn top-level-scan-item [scan-name]
  (let [scan-sym (-> scan-name (str/lower-case) (str/replace "_" "-") symbol)]
    (if-let [scan-resolved (resolve scan-sym)]
      scan-resolved
      (intern *ns* scan-sym (atom {})))))

(defn scanner-subscriptions-with-ids [config scanner-subscriptions]

  (let [scan-types (->> config :scanners (map #(select-keys % [:scan-name :tag])))]

    (reduce (fn [acc {:keys [scan-name tag]}]
              (let [next-id (next-reqid acc)
                    subscription {::reqid next-id
                                  ::scan-name scan-name
                                  ::scan-value {}
                                  ::tag tag}]
                (conj acc subscription)))
            scanner-subscriptions
            scan-types)))

(defn consume-subscriber [scan-key sink-channel subscriber-chan]
  (go-loop [r1 (<! subscriber-chan)]
    (let [{:keys [req-id symbol rank] :as val} (select-keys r1 [:req-id :symbol :rank])]
      (if (and r1 rank)

        ;; TODO 2 - change from atoms to channels
        #_(swap! scan-atom assoc rank val)
        (>! sink-channel {scan-key {rank val}})))

    (recur (<! subscriber-chan))))

(defn scanner-start [client publication sink-channel config]

  (let [default-instrument (-> config :stocks :default-instrument)
        default-location (-> config :stocks :default-location)
        scanner-subscriptions-init []
        scanner-subscriptions (scanner-subscriptions-with-ids config scanner-subscriptions-init)]

    (doseq [{:keys [::reqid ::scan-name ::tag] :as val} scanner-subscriptions
            :let [subscriber (chan)]]

      (let [;; scan-var (top-level-scan-item scan-name)
            ;; scan-atom (var-get scan-var)
            scan-key' (scan-key scan-name)]

        (ei/scanner-subscribe reqid client default-instrument default-location scan-name)
        (sub publication reqid subscriber)

        ;; TODO 1 - change from atoms to channels
        (consume-subscriber scan-key' sink-channel subscriber)))

    scanner-subscriptions))

(defn scanner-stop [])

(comment

  (def client (-> system.repl/system :ewrapper :ewrapper :client))
  (def publisher (-> system.repl/system :ewrapper :ewrapper :publisher))
  (def publication
    (pub publisher #(:req-id %)))

  (def scanner-subscriptions (scanner-start client publication config))
  (pprint scanner-subscriptions)


  ;; HISTORICALDATA
  ;; (def historical-atom (atom {}))
  ;; (def historical-subscriptions (historical-start 4002 client publication historical-atom))

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

  #_(pprint (take 6 (->> @historical-atom
                       (sort-by first)
                       (remove (fn [[k {:keys [date] :as v}]]
                                 (or (nil? k)
                                     (str/starts-with? date "finished-")))))))

  #_(def historical-final (->> @historical-atom
                             (sort-by first)
                             (remove (fn [[k {:keys [date] :as v}]]
                                       (or (nil? k)
                                           (str/starts-with? date "finished-"))))))

  #_(pprint (take 7 historical-final))

  ;; 1. write to edn
  ;; (spit "tesla-historical-20170901-20170915.edn" @historical-atom)
  ;; (spit "tesla-historical-20170601-20170928.edn" (pr-str historical-final))

  ;; 2. write to json
  ;; (require '[clojure.data.json :as json])
  ;; (spit "tesla-historical-20170601-20170928.json" (json/write-str historical-final))


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
