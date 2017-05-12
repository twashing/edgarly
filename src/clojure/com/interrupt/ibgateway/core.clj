(ns com.interrupt.ibgateway.core
  (:require  [com.stuartsierra.component :as component]
             [system.repl :refer [set-init! init start stop reset refresh system]]
             [system.components.repl-server :refer [new-repl-server]]
             [com.interrupt.component.ewrapper :refer [new-ewrapper]]
             [com.interrupt.component.ewrapper-impl :as ei]
             [clojure.string :as str]
             [clojure.core.async :refer [chan >! <! merge go go-loop pub sub unsub-all sliding-buffer]]
             [clojure.core.match :refer [match]]
             [clojure.spec :as s]
             [clojure.spec.gen :as sg]
             [clojure.spec.test :as st]
             [clojure.future :refer :all]
             [com.rpl.specter :refer [transform select ALL]]
             [clojure.set :as cs]
             [clojure.math.combinatorics :as cmb]
             [clojure.pprint :refer [pprint]])
  (:import [java.util.concurrent TimeUnit]))


(defn system-map []
  (component/system-map
   :nrepl (new-repl-server 7888 "0.0.0.0")  ;; useful when operating to the cloud
   :ewrapper (new-ewrapper)))

(set-init! #'system-map)
(defn start-system [] (start))
(defn stop-system [] (stop))

(def config
  {:stocks {:default-instrument "STK"
            :default-location "STK.US.MAJOR"}

   :scanners [{:scan-name "HIGH_OPT_IMP_VOLAT"
               :scan-value {}
               :tag :volatility}
              {:scan-name "HIGH_OPT_IMP_VOLAT_OVER_HIST"
               :scan-value {}
               :tag :volatility}
              {:scan-name "HOT_BY_VOLUME"
               :scan-value {}
               :tag :volume}
              {:scan-name "TOP_VOLUME_RATE"
               :scan-value {}
               :tag :volume}
              {:scan-name "HOT_BY_OPT_VOLUME"
               :scan-value {}
               :tag :volume}
              {:scan-name "OPT_VOLUME_MOST_ACTIVE"
               :scan-value {}
               :tag :volume}
              {:scan-name "COMBO_MOST_ACTIVE"
               :scan-value {}
               :tag :volume}
              {:scan-name "MOST_ACTIVE_USD"
               :scan-value {}
               :tag :price}
              {:scan-name "HOT_BY_PRICE"
               :scan-value {}
               :tag :price}
              {:scan-name "TOP_PRICE_RANGE"
               :scan-value {}
               :tag :price}
              {:scan-name "HOT_BY_PRICE_RANGE"
               :scan-value {}
               :tag :price}]})

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

;; TODO - replace with kafka + stream processing asap
(defn top-level-scan-item [scan-name]
  (let [scan-sym #spy/d (-> scan-name (str/lower-case) (str/replace "_" "-") symbol)]
    (if-let [scan-resolved (resolve scan-sym)]
      scan-resolved
      (intern *ns* scan-sym (atom {})))))

(defn scanner-subscriptions-with-ids [confg scanner-subscriptions]

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

(defn consume-subscriber [scan-atom subscriber-chan]
  (go-loop [r1 nil]
    (let [{:keys [req-id symbol rank] :as val} (select-keys r1 [:req-id :symbol :rank])]
      (if (and r1 rank)
        (swap! scan-atom assoc rank val)))

    (recur (<! subscriber-chan))))

(defn scanner-start [client publication config]

  (let [default-instrument (-> config :stocks :default-instrument)
        default-location (-> config :stocks :default-location)
        scanner-subscriptions-init []
        scanner-subscriptions (scanner-subscriptions-with-ids config scanner-subscriptions-init)]

    (doseq [{:keys [::reqid ::scan-name ::tag] :as val} scanner-subscriptions
            :let [subscriber (chan)]]

      ;; TODO - replace with kafka + stream processing asap
      (let [scan-var #spy/d (top-level-scan-item scan-name)
            scan-atom #spy/d (var-get scan-var)]
        (ei/scanner-subscribe reqid client default-instrument default-location scan-name)
        (sub publication reqid subscriber)
        (consume-subscriber scan-atom subscriber)))

    scanner-subscriptions))

(defn scanner-stop [])

(comment

  ;; TODO
  ;;
  ;; write (Transit) to Kafka
  ;; read (Transit) from Kafka
  ;; feed to analysis

  (def client (-> system.repl/system :ewrapper :ewrapper :client))
  (def publisher (-> system.repl/system :ewrapper :ewrapper :publisher))
  (def publication
    (pub publisher #(:req-id %)))

  (def scanner-subscriptions (scanner-start client publication config))

  (pprint scanner-subscriptions)

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
                scan-subsets #spy/d (map (fn [sname]
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
    (filter (fn [e]
              (and (> (count (:intersection e)) 1)
                   (some #{"one" "two"} (:names e))
                   (some #{"three" "four" "five" "six" "seven"} (:names e))
                   (some #{"eight" "nine" "ten" "eleven"} (:names e))))
            sorted-intersections)))

(defn historical-start [])
(defn historical-stop [])

(defn market-start [])
(defn market-stop [])

(defn open-request-ids [])

(defn -main [& args]
  (Thread/sleep 5000) ;; a hack, to ensure that the tws machine is available, before we try to connect to it.
  (start-system))

