(ns com.interrupt.analysis.lagging
  (:require [clojure.java.io :as io]))


(defn average [list-sum list-count]
  (/ list-sum list-count))

(defn simple-moving-average [options tick-window tick-list]

  (let [;; calculate how far back the window can start
        start-index tick-window

        {input-key :input
         output-key :output
         etal-keys :etal2
         :or {input-key :close
              output-key :close-average
              etal-keys [:close :date]}} options]

    ;; calculate Simple Moving Average for each slot there's a window
    (reduce (fn [rslt ech]

              (let [tsum (reduce (fn [rslt inp]
                                   (let [ltprice (input-key inp)]
                                     (+ ltprice rslt))) 0 ech)

                    taverage (average tsum (count ech))]

                (concat (into [] rslt)
                        [(merge

                          ;; will produce a map of etal-keys, with associated values in ech
                          (zipmap etal-keys
                                  (map #(% (last ech)) etal-keys))

                          ;; and merge the output key to the map
                          {output-key taverage
                           :population ech})])))

            []  ;; ma-list
            (partition tick-window 1 tick-list))))

(defn exponential-moving-average

  ([options tick-window tick-list]

   (exponential-moving-average options tick-window tick-list (simple-moving-average nil tick-window tick-list)))

  ([options tick-window tick-list sma-list]

   ;; 1. calculate 'k'
   ;; k = 2 / N + 1
   ;; N = number of days
   (let [k (/ 2 (+ tick-window 1))

         {input-key :input
          output-key :output
          etal-keys :etal
          :or {input-key :close
               output-key :close-exponential
               etal-keys [:close :date]}} options]

     ;; 2. get the simple-moving-average for a given tick - 1
     (reduce (fn [rslt ech]

               ;; 3. calculate the EMA ( for the first tick, EMA(yesterday) = MA(yesterday) )
               (let [;; price(today)

                     ltprice (input-key ech)

                     ;; EMA(yesterday)
                     ema-last (if (output-key (last rslt))
                                (output-key (last rslt))
                                (input-key ech))

                     ;; ** EMA now = price(today) * k + EMA(yesterday) * (1 - k)
                     ema-now (+ (* k ltprice)
                                (* ema-last (- 1 k)))]

                 (concat (into [] rslt)

                         ;; and prepend the result to our running list
                         [(merge

                           ;; will produce a map of etal-keys, with associated values in ech
                           (zipmap etal-keys
                                   (map #(% ech) etal-keys))

                           ;; and merge the output key to the map
                           {output-key ema-now})])))
             []
             sma-list))))

(defn bollinger-band

  ([tick-window tick-list]
   (bollinger-band tick-window tick-list (simple-moving-average nil tick-window tick-list)))

  ([tick-window tick-list sma-list]

   (reduce (fn [rslt ech]

             (let [;; get the Moving Average
                   ma (:close-average ech)

                   ;; work out the mean
                   mean (/ (reduce (fn [rslt ech]
                                     (+ (:close ech)
                                        rslt))
                                   0
                                   (:population ech))
                           (count (:population ech)))

                   ;; Then for each number: subtract the mean and square the result (the squared difference)
                   sq-diff-list (map (fn [ech]
                                       (let [diff (- mean (:close ech))]
                                         (* diff diff)))
                                     (:population ech))

                   variance (/ (reduce + sq-diff-list) (count (:population ech)))
                   standard-deviation (. Math sqrt variance)]
               (concat (into [] rslt)
                       [{:close (:close ech)
                         :date (:date ech)
                         :upper-band (+ ma (* 2 standard-deviation))
                         :lower-band (- ma (* 2 standard-deviation))}])))
           []
           sma-list)))

(comment

  (require '[clojure.tools.reader.edn :as edn]
           '[clojure.java.io :as io]
           '[spyscope.core])

  (def tick-list (edn/read-string (slurp (io/resource "tesla-historical-20170819-20170829.edn"))))


  ;; == LAGGING (open, close, high, low)

  (def tick-list-distilled (map second tick-list))

  (def result-simple-moving-average (simple-moving-average nil 40 tick-list-distilled))
  (def result-exponential-moving-average (exponential-moving-average nil 40 tick-list-distilled result-simple-moving-average))
  (def result-bollinger-band (bollinger-band 40 tick-list-distilled))

  (clojure.pprint/pprint (take 4 tick-list-distilled))
  (clojure.pprint/pprint (first result-simple-moving-average))
  (clojure.pprint/pprint (first (simple-moving-average nil 40 tick-list-distilled)))

  (clojure.pprint/pprint (take 2 (map #(dissoc % :population) result-simple-moving-average)))
  (clojure.pprint/pprint (take 2 result-exponential-moving-average))
  (clojure.pprint/pprint (take 2 result-bollinger-band))

  #_(require '[clojure.data.json :as json])
  #_(spit "tesla-historical-20170819-20170829-with-lagging.edn" (pr-str tick-list-with-lagging))
  #_(spit "tesla-historical-20170819-20170829-with-lagging.json" (json/write-str tick-list-with-lagging))


  ;; == LEADING
  (require '[com.interrupt.analysis.leading :as led])
  (def result-macd (led/macd nil 40 tick-list-distilled result-simple-moving-average))
  (def result-stochastic-oscillator (led/stochastic-oscillator 14 3 3 tick-list-distilled))


  (clojure.pprint/pprint (take 2 result-macd))
  (clojure.pprint/pprint (take 2 result-stochastic-oscillator))


  ;; == CONFIRMING
  (require '[com.interrupt.analysis.confirming :as cnf])

  (def result-on-balance-volume (cnf/on-balance-volume nil tick-list-distilled))
  (def result-relative-strength-index (cnf/relative-strength-index 40 tick-list-distilled))

  (clojure.pprint/pprint (take 20 result-on-balance-volume))
  (clojure.pprint/pprint (take 2 result-relative-strength-index))


  (def tick-list-with-analytics
    (map (fn [tl

             {close-average :close-average simple-date :date}
             {close-exponential :close-exponential exponential-date :date}
             {upper-band :upper-band lower-band :lower-band bollinger-date :date}

             {close-macd :close-macd ema-signal :ema-signal histogram :histogram macd-date :date}
             {highest-price :highest-price lowest-price :lowest-price stochastic-oscillator-date :date K :K D :D}

             {obv :obv volume :volume obv-date :date}
             {rs :rs rsi :rsi rsi-date :date}]

           {:nominal/nominal tl

            :lagging/simple-moving-average {:date simple-date
                                            :close-average close-average}

            :lagging/exponential-moving-average {:date exponential-date
                                                 :close-exponential close-exponential}

            :lagging/bollinger-band {:date bollinger-date
                                     :upper-band upper-band
                                     :lower-band lower-band}

            :leading/macd {:date macd-date
                           :close-macd close-macd
                           :ema-signal ema-signal
                           :histogram histogram}

            :leading/stochastic-oscillator {:date stochastic-oscillator-date
                                            :highest-price highest-price
                                            :lowest-price lowest-price
                                            :K K
                                            :D D}

            :confirming/on-balance-volume {:date obv-date
                                           :obv obv
                                           :volume volume}

            :confirming/relative-strength-index {:date rsi-date
                                                 :rs rs
                                                 :rsi rsi}})

         tick-list-distilled

         result-simple-moving-average
         result-exponential-moving-average
         result-bollinger-band

         result-macd
         result-stochastic-oscillator
         result-on-balance-volume
         result-relative-strength-index))

  (clojure.pprint/pprint (first tick-list-with-analytics))

  (require '[clojure.data.json :as json])
  (spit "tesla-historical-20170819-20170829-with-analytics.edn" (pr-str tick-list-with-analytics))
  (spit "tesla-historical-20170819-20170829-with-analytics.json" (json/write-str tick-list-with-analytics))


  (require '[com.interrupt.signal.lagging :as slag])


  (clojure.pprint/pprint (first tick-list-distilled))
  (clojure.pprint/pprint (first result-simple-moving-average))
  (clojure.pprint/pprint (first result-exponential-moving-average))

  (def grouped-lagging-analytics (remove (fn [x]
                                           (< (count (second x)) 3))
                                         (sort-by first
                                                  (group-by :date (concat tick-list-distilled result-simple-moving-average result-exponential-moving-average)))))

  (clojure.pprint/pprint (first grouped-lagging-analytics))
  (clojure.pprint/pprint (take 20 (map first grouped-lagging-analytics)))


  (def lagging-tick-list (map (comp first second) grouped-lagging-analytics))
  (def lagging-simple-list (map (comp second second) grouped-lagging-analytics))
  (def lagging-exponential-list (map (fn [x] (nth (second x) 2))
                                     grouped-lagging-analytics))

  (clojure.pprint/pprint (take 6 lagging-tick-list))
  (clojure.pprint/pprint (take 6 lagging-simple-list))
  (clojure.pprint/pprint (take 6 lagging-exponential-list))


  (def result-moving-average-signals
    (slag/moving-averages 40 lagging-tick-list lagging-simple-list lagging-exponential-list))

  (clojure.pprint/pprint
   (filter :signals (slag/moving-averages 40 lagging-tick-list lagging-simple-list lagging-exponential-list)))

  (clojure.pprint/pprint
   (take 100 (slag/join-averages 40 lagging-tick-list lagging-simple-list lagging-exponential-list)))


  #_(def one (map

            (fn [x]
              {:close (-> x :nominal :close)
               :date (-> x :nominal :date)
               :lagging (:lagging x)})

            tick-list-with-analytics))
  #_(clojure.pprint/pprint (take 10 one))
  #_(take 10 tick-list-distilled)


  )
