(ns com.interrupt.signal.lagging
  (:require [com.interrupt.analysis.lagging :as analysis]
            [com.interrupt.analysis.confirming :as confirming]
            [com.interrupt.signal.common :as common]
            [clojure.tools.logging :as log]))


(defn join-averages
  "Create a list where i) tick-list ii) sma-list and iii) ema-list are overlaid.

   ** This function assumes the latest tick is on the right **"

  ([tick-window tick-list]

   (let [sma-list (analysis/simple-moving-average nil tick-window tick-list)
         ema-list (analysis/exponential-moving-average nil tick-window tick-list sma-list)]
     (join-averages tick-window tick-list sma-list ema-list)))

  ([tick-window tick-list sma-list ema-list]
   (map (fn [titem sitem eitem]

          ;; 1. ensure that we have the :date for simple and exponential items
          ;; 2. ensure that all 3 time items line up
          (if (and (and (not (nil? (:date sitem)))
                        (not (nil? (:date eitem))))
                   (= (:date titem) (:date sitem) (:date eitem)))

            {:date (:date titem)
             :close (:close titem)
             :close-average (:close-average sitem)
             :close-exponential (:close-exponential eitem)}

            nil))

        tick-list
        sma-list
        ema-list)))

(defn moving-averages
  "Takes baseline time series, along with 2 other moving averages.

   Produces a list of signals where the 2nd moving average overlaps (abouve or below) the first.
   By default, this function will produce a Simple Moving Average and an Exponential Moving Average.

   ** This function assumes the latest tick is on the left**"

  ([tick-window tick-list]

   (let [sma-list (analysis/simple-moving-average nil tick-window tick-list)
         ema-list (analysis/exponential-moving-average nil tick-window tick-list sma-list)]
     (moving-averages tick-window tick-list sma-list ema-list)))

  ([tick-window tick-list sma-list ema-list]

   ;; create a list where i) tick-list ii) sma-list and iii) ema-list are overlaid
   (let [joined-list (join-averages tick-window tick-list sma-list ema-list)
         partitioned-join (partition 2 1 joined-list)
         start-list []
         ;; start-list (into '() (repeat tick-window nil))
         ]


     ;; find time points where ema-list (or second list) crosses over the sma-list (or 1st list)
     (reduce (fn [rslt ech]

               (let [fst (first ech)

                     snd (second ech)

                     ;; in the latest element, has the ema crossed abouve the sma from the previous element
                     signal-up (and (> (:close-exponential snd) (:close-average snd))
                                           (< (:close-exponential fst) (:close-average fst)))

                     ;; in the first element, has the ema crossed below the sma from the second element
                     signal-down (and (< (:close-exponential snd) (:close-average snd))
                                      (> (:close-exponential fst) (:close-average fst)))

                     raw-data fst]

                 ;; return either i) :up signal, ii) :down signal or iii) nothing, with just the raw data
                 (if signal-up
                   (concat (into [] rslt)
                           [(assoc raw-data :signals [{:signal :up
                                                       :why :moving-average-crossover
                                                       :arguments [fst snd]}])])
                   (if signal-down
                     (concat (into [] rslt)
                             [(assoc raw-data :signals [{:signal :down
                                                         :why :moving-average-crossover
                                                         :arguments [fst snd]}])])
                     (concat (into [] rslt)
                             [raw-data])))))
             start-list
             partitioned-join))))

(defn sort-bollinger-band [bband]
  (let [diffs (map (fn [inp]
                     (assoc inp :difference (- (:upper-band inp) (:lower-band inp))))
                   (remove nil? bband))]
    (sort-by :difference diffs)))

(defn bollinger-band

  "Implementing signals for analysis/bollinger-band. Taken from these videos:
     i. http://www.youtube.com/watch?v=tkwUOUZQZ3s
     ii. http://www.youtube.com/watch?v=7PY4XxQWVfM


      A. when the band width is very low, can indicate that price will breakout sooner than later;

      i. MA is in an UP or DOWN market
      ii. check for narrow bollinger band width ; less than the most previous narrow band width
      iii. close is outside of band, and previous swing high/low is inside the band


       B. when the band width is very high (high volatility); can mean that the trend is ending soon; can i. change direction or ii. consolidate

       i. MA is in a sideways (choppy) market -> check if many closes that are abouve or below the bollinger band
       ii. check for a wide bollinger band width ; greater than the most previous wide band
       iii. RSI Divergence; i. price makes a higher high and ii. rsi devergence makes a lower high iii. and divergence should happen abouve the overbought line
       iv. entry signal -> check if one of next 3 closes are underneath the priors (or are in the opposite direction)

   ** This function assumes the latest tick is on the left**"
  ([tick-window tick-list]
   (let [sma-list (analysis/simple-moving-average nil tick-window tick-list)]
     (bollinger-band tick-window tick-list sma-list)))

  ([tick-window tick-list sma-list]
   (let [bband (analysis/bollinger-band tick-window tick-list sma-list)]
     (bollinger-band tick-window tick-list sma-list bband)))

  ([tick-window tick-list sma-list bband]

   (let []

     (reduce (fn [rslt ech-list]

               (let [;; track widest & narrowest band over the last 'n' ( 3 ) ticks
                     sorted-bands (sort-bollinger-band ech-list)
                     most-narrow (take 3 sorted-bands)
                     most-wide (take-last 3 sorted-bands)


                     partitioned-list (partition 2 1 (remove nil? ech-list))

                     upM? (common/up-market? 10 (remove nil? partitioned-list))
                     downM? (common/down-market? 10 (remove nil? partitioned-list))

                     ;; ... TODO - determine how far back to look (defaults to 10 ticks) to decide on an UP or DOWN market
                     ;; ... TODO - does tick price fluctuate abouve and below the MA
                     ;; ... TODO - B iv. entry signal -> check if one of next 3 closes are underneath the priors
                     ;;            (or are in the opposite direction)
                     side-market? (if (and (not upM?)
                                           (not downM?))
                                    true
                                    false)

                     ;; find last 3 peaks and valleys
                     peaks-valleys (common/find-peaks-valleys nil (remove nil? ech-list))
                     peaks (:peak (group-by :signal peaks-valleys))
                     valleys (:valley (group-by :signal peaks-valleys))]


                 (if (empty? (remove nil? ech-list))

                   (concat (into [] rslt) [nil])

                   (if (or upM? downM?)

                     ;; A.
                     (let [latest-diff (- (:upper-band (last ech-list)) (:lower-band (last ech-list)))
                           less-than-any-narrow? (some (fn [inp] (< latest-diff (:difference inp))) most-narrow)]

                       (if less-than-any-narrow?

                         ;; entry signal -> close is outside of band, and previous swing high/low is inside the band
                         (if upM?

                           (if (and (< (:close (last ech-list)) (:lower-band (last ech-list)))
                                    (> (:close (last valleys)) (:lower-band (last (some #(= (:date %)
                                                                                            (:date (last valleys)))
                                                                                        ech-list)))))

                             (concat (into [] rslt)
                                     [(assoc (last ech-list)
                                             :signals [{:signal :down
                                                        :why :bollinger-close-abouve
                                                        :arguments [ech-list valleys]}])])

                             (concat (into [] rslt)
                                     [(last ech-list)]))

                           (if (and (> (:close (last ech-list)) (:upper-band (last ech-list)))
                                    (< (:close (last peaks)) (:upper-band (last (some #(= (:date %)
                                                                                          (:date (last peaks))))
                                                                                ech-list))))

                             (concat (into [] rslt)
                                     [(assoc (last ech-list)
                                             :signals [{:signal :up
                                                        :why :bollinger-close-below
                                                        :arguments [ech-list peaks]}])])

                             (concat (into [] rslt)
                                     [(last ech-list)])))))

                     ;; B.
                     (let [latest-diff (- (:upper-band (last ech-list)) (:lower-band (last ech-list)))
                           more-than-any-wide? (some (fn [inp] (> latest-diff (:difference inp))) most-wide)]

                       (if more-than-any-wide?

                         ;; B iii RSI Divergence
                         (let [OVER_BOUGHT 80
                               OVER_SOLD 20
                               rsi-list (confirming/relative-strength-index 14 ech-list)

                               ;; i. price makes a higher high and
                               higher-highPRICE? (if (empty? peaks)
                                                   false
                                                   (> (:close (last ech-list))
                                                      (:close (last peaks))))

                               ;; ii. rsi devergence makes a lower high
                               lower-highRSI? (if (or (empty? peaks)
                                                      (some #(nil? (:date %)) rsi-list)
                                                      (not (nil? rsi-list)))
                                                false
                                                (< (:rsi (last rsi-list))
                                                   (:rsi
                                                    (last
                                                     (filter
                                                      (fn [inp]
                                                        (log/info
                                                         "... signal.lagging/bollinger-band > lower-lowRSI? > rsi-list > ech[" inp "]")
                                                        (= (:date inp)
                                                           (:date (last peaks))))
                                                      rsi-list)))))

                               ;; iii. and divergence should happen abouve the overbought line
                               divergence-overbought? (> (:rsi (last rsi-list))
                                                         OVER_BOUGHT)


                               ;; i. price makes a lower low
                               lower-highPRICE? (if (or (empty? valleys)
                                                        (some #(nil? (:date %)) rsi-list))
                                                  false
                                                  (< (:close (last ech-list))
                                                     (:close (last valleys))))

                               higher-highRSI? (if (or (empty? valleys)
                                                       (not (nil? rsi-list)))
                                                 false
                                                 (> (:rsi (last rsi-list))
                                                    (:rsi
                                                     (last
                                                      (filter
                                                       (fn [inp]
                                                         (log/info
                                                          "... signal.lagging/bollinger-band > higher-highRSI? > RSI-LIST > ech[" inp "]")
                                                         (= (:date inp)
                                                            (:date (last valleys))))
                                                       rsi-list)))))

                               divergence-oversold? (< (:rsi (last rsi-list))
                                                       OVER_SOLD)]

                           (if (and higher-highPRICE? lower-highRSI? divergence-overbought?)

                             (concat (into [] rslt)
                                     [(assoc (last ech-list)
                                             :signals [{:signal :down
                                                        :why :bollinger-divergence-overbought
                                                        :arguments [peaks ech-list rsi-list]}])])

                             (if (and lower-highPRICE? higher-highRSI? divergence-oversold?)

                               (concat (into [] rslt)
                                       [(assoc (last ech-list) :signals [{:signal :up
                                                                          :why :bollinger-divergence-oversold
                                                                          :arguments [valleys ech-list rsi-list]}])])

                               (concat (into [] rslt)
                                       [(last ech-list)]))))

                         (concat (into [] rslt)
                                 [(last ech-list)])))))

                 (concat (into [] rslt)
                         [(last ech-list)])))
             []
             (partition tick-window 1 bband)))))
