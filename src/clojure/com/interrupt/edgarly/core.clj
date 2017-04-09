(ns com.interrupt.edgarly.core
  (:require  [com.stuartsierra.component :as component]
             [system.repl :refer [set-init! init start stop reset refresh system]]
             [system.components.repl-server :refer [new-repl-server]]
             [com.interrupt.component.ewrapper :refer [new-ewrapper]]
             [clojure.spec :as s]
             [clojure.spec.gen :as sg]
             [clojure.spec.test :as st]
             [clojure.future :refer :all]

             [clojure.pprint :refer [pprint]])
  (:import [java.util.concurrent TimeUnit]))

(def config {:default-instrument "stk"
             :default-location "stk.us.major"

             :high-opt-imp-volat "high_opt_imp_volat"
             :high-opt-imp-volat-over-hist "high_opt_imp_volat_over_hist"
             :hot-by-volume "hot_by_volume"
             :top-volume-rate "top_volume_rate"
             :hot-by-opt-volume "HOT_BY_OPT_VOLUME"
             :opt-volume-most-active "OPT_VOLUME_MOST_ACTIVE"
             :combo-most-active "COMBO_MOST_ACTIVE"
             :most-active-usd "MOST_ACTIVE_USD"
             :hot-by-price "HOT_BY_PRICE"
             :top-price-range "TOP_PRICE_RANGE"
             :hot-by-price-range "HOT_BY_PRICE_RANGE"})


(defn system-map []
  (component/system-map
   :nrepl (new-repl-server 7888 "0.0.0.0")  ;; useful when operating to the cloud
   :ewrapper (new-ewrapper)))

(set-init! #'system-map)


;; ===
(defn average [list-sum list-count]
  (/ list-sum list-count))

(s/fdef average
        :args (s/and (s/cat :list-sum float? :list-count integer?)
                     #(not (zero? (:list-count %))))
        :ret number?)


;; ===
(def scanner-subscriptions [{::name "one"
                             ::val {}
                             ::tag :volatility
                             ::reqid 1}
                            {::name "three"
                             ::val {}
                             ::tag :volume
                             ::reqid 3}
                            {::name "eight"
                             ::val {}
                             ::tag :price
                             ::reqid 8}])

(s/def ::reqid pos-int?)
(s/def ::subscription-element (s/keys :req [::reqid]))
(s/def ::subscriptions (s/coll-of ::subscription-element))

(defn next-reqid [scanner-subscriptions]
  #spy/d scanner-subscriptions
  :foo
  #_(let [reqids (sort (map :reqid scanner-subscriptions))
        reqids-count (count reqids)
        first-id (first reqids)

        contiguous-numbers-from-first (range first-id (+ first-id reqids-count))
        gap-numbers (remove nil?
                            (map (fn [left right]
                                   (if-not (= left right)
                                     right
                                     nil))
                                 reqids
                                 contiguous-numbers-from-first))]

    (if-not (empty? gap-numbers)
      (first gap-numbers)
      (+ 1 (last reqids)))

    ))

(s/fdef next-reqid
        :args (s/cat :subscriptions ::subscriptions)
        :ret (s/fspec :args (s/cat :y number?)))

;; (s/exercise-fn `next-reqid)
;; (st/check `next-reqid)

;; ===

(defn ranged-rand
  "Returns random int in range start <= rand < end"
  [start end]
  (+ start (long (rand (- end start)))))

(s/fdef ranged-rand
        :args (s/and (s/cat :start int? :end int?)
                     #(< (:start %) (:end %)))
        :ret int?
        #_:fn #_(s/and #(>= (:ret %) (-> % :args :start))
                   #(< (:ret %) (-> % :args :end))))

;; (s/exercise-fn `ranged-rand)
;; (st/check `ranged-rand)


(comment

  ;; compares existing list of request IDs, to the set of numbers from 1 to the highest number
  ;;
  ;; (3 5 15) -> 1 should be the first gap
  ;; (1 2 3 4 5 6 7 8 9 10 11 12 13 14)
  ;;
  ;; (1 2 3 5) -> 4 should be the first gap
  ;; (1 2 3 4 5)
  ;;
  ;; (1 2 3 4 5 6 7 8 9 10)
  ;; (1 2 10) -> 3 should be the first gap
  ;;
  ;; finds the first gap number
  ;; can be in first position
  ;; gap can be on left or right side
  ;; handles nil and empty sets

  (next-reqid scanner-subscriptions)
  (next-reqid [])
  (next-reqid nil))

(def volat-one (atom {}))
(def volat-two (atom {}))

(def volume-three (atom {}))
(def volume-four (atom {}))
(def volume-five (atom {}))
(def volume-six (atom {}))
(def volume-seven (atom {}))

(def price-eight (atom {}))
(def price-nine (atom {}))
(def price-ten (atom {}))
(def price-eleven (atom {}))


(defn start-system [] (start))
(defn stop-system [] (stop))

(defn scanner-start [])
(defn scanner-stop [])

(defn historical-start [])
(defn historical-stop [])

(defn market-start [])
(defn market-stop [])

(defn open-request-ids [])

(defn -main [& args]
  (Thread/sleep 000) ;; a hack, to ensure that the tws machine is available, before we try to connect to it.
  (start-system))

(comment

  (pprint system)
  (keys system)

  (pprint (:ewrapper system))

  )

(comment

  (def client (-> system.repl/system :ewrapper :ewrapper :client))
  (def publisher (-> system.repl/system :ewrapper :ewrapper :publisher))

  ;; ===
  ;; Stock scanners
  (def default-instrument (:default-instrument config))
  (def default-location (:default-location config))

  ;; Volatility
  (scanner-subscribe 1 client default-instrument default-location (:high-opt-imp-volat config))
  (scanner-subscribe 2 client default-instrument default-location (:high-opt-imp-volat-over-hist config))

  ;; Volume
  (scanner-subscribe 3 client default-instrument default-location (:hot-by-volume config))
  (scanner-subscribe 4 client default-instrument default-location (:top-volume-rate config))
  (scanner-subscribe 5 client default-instrument default-location (:hot-by-opt-volume config))
  (scanner-subscribe 6 client default-instrument default-location (:opt-volume-most-active config))
  (scanner-subscribe 7 client default-instrument default-location (:combo-most-active config))

  ;; Price Change
  (scanner-subscribe 8 client default-instrument default-location (:most-active-usd config))
  (scanner-subscribe 9 client default-instrument default-location (:hot-by-price config))
  (scanner-subscribe 10 client default-instrument default-location (:top-price-range config))
  (scanner-subscribe 11 client default-instrument default-location (:hot-by-price-range config))


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
            sorted-intersections))

  ;; Unsubscribe
  (scanner-unsubscribe 1 client)
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
