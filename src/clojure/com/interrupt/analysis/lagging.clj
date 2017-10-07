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
                                  (map #(% (first ech)) etal-keys))

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

  ;; ** CRITICAL to reverse the ticklist
  (def tick-list-distilled (map second tick-list))

  (def result-simple-moving-average
    (simple-moving-average nil 40 tick-list-distilled))
  (def result-exponential-moving-average
    (exponential-moving-average nil 40 tick-list-distilled result-simple-moving-average))
  (def result-bollinger-band
    (bollinger-band 40 tick-list-distilled))

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

  #_(def result-on-balance-volume (cnf/on-balance-volume [latest-tick tick-list]))
  (def result-relative-strength-index (cnf/relative-strength-index 40 tick-list-distilled))

  #_(clojure.pprint/pprint (take 2 result-on-balance-volume))
  (clojure.pprint/pprint (take 2 result-relative-strength-index))


  (def tick-list-with-analytics
    (map (fn [tl

             {close-average :close-average}
             {close-exponential :close-exponential}
             {upper-band :upper-band lower-band :lower-band}

             {close-macd :close-macd ema-signal :ema-signal histogram :histogram}
             {highest-price :highest-price lowest-price :lowest-price K :K D :D}

             {rs :rs rsi :rsi}]

           {:nominal tl
            :lagging {:close-average close-average
                      :close-exponential close-exponential
                      :upper-band upper-band
                      :lower-band lower-band}

            :leading {:close-macd close-macd
                      :ema-signal ema-signal
                      :histogram histogram

                      :highest-price highest-price
                      :lowest-price lowest-price
                      :K K
                      :D D}

            :confirming {:rs rs
                         :rsi rsi}})

         tick-list-distilled

         result-simple-moving-average
         result-exponential-moving-average
         result-bollinger-band

         result-macd
         result-stochastic-oscillator
         #_result-on-balance-volume
         result-relative-strength-index))

  (clojure.pprint/pprint
   (first tick-list-with-analytics))


  (def one (map

            (fn [x]
              {:close (-> x :nominal :close)
               :date (-> x :nominal :date)
               :lagging (:lagging x)})

            tick-list-with-analytics))

  (clojure.pprint/pprint (take 10 one))

  (take 10 tick-list-distilled)

  )
