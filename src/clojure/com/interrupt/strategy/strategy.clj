(ns com.interrupt.strategy.strategy
  (:require [com.interrupt.signal.common :as common]
            [com.interrupt.signal.leading :as sleading]))


(defn price-increase? [tick-list]

  (let [fst (first tick-list)
        snd (second tick-list)]

    (> (:close fst) (:close snd))))

(defn price-below-sma? [tick-list signals-ma]

  (let [p-tick (first tick-list)
        sma-tick (first signals-ma)]

    (< (:close p-tick) (:close-average sma-tick))))

(defn no-price-oscillation? [tick-list]

  (let [
        ;; find last 3 peaks and valleys
        peaks-valleys (common/find-peaks-valleys nil (remove nil? tick-list))
        peaks (take 2 (:peak (group-by :signal peaks-valleys)))
        valleys (take 2 (:valley (group-by :signal peaks-valleys)))]

    (and

     ;; ensure price isn't oscillating between peaks and valleys
     (not (and (every? #(= (-> peaks first :close)
                           (:close %)) peaks)
               (every? #(= (-> valleys first :close)
                           (:close %)) valleys)))

     ;; ensure latest price isn't equal to the previous peak
     (not (= (:close (first tick-list))
             (:close (first peaks)))))))

(defn price-cross-abouve-sma?
  ([tick-list sma-list]
   (price-cross-abouve-sma? tick-list sma-list nil))
  ([tick-list sma-list options]

   (let [ftick (first tick-list)
         ntick (second tick-list)

         fsma (first (filter (fn [inp] (= (:date ftick)
                                         (:date inp)))
                             sma-list))
         nsma (first (filter (fn [inp] (= (:date ntick)
                                         (:date inp)))
                             sma-list))

         {compare-key :compare-key
          :or {compare-key :close-average}} options]

     (and (<= (:close ntick) (compare-key nsma))
          (> (:close ftick) (compare-key fsma))))))

(defn bollinger-price-below? [tick-list signals-bollinger]

  (let [p-tick (first tick-list)
        b-ticks (take 2 signals-bollinger)]

    (-> (some (fn [inp]
                (< (:close p-tick)
                   (:lower-band inp)))
              b-ticks)
        nil?
        not)))

(defn bollinger-was-narrower? [signals-bollinger]

  (let [b1 (first signals-bollinger)
        b-first (assoc b1 :difference (- (:upper-band b1)
                                         (:lower-band b1)))

        b2 (take 2 (rest signals-bollinger))
        b-rest (map (fn [inp]
                      (assoc inp :difference (- (:upper-band inp)
                                                (:lower-band inp))))
                    b2)]

    (-> (some (fn [inp]
                (>= (:difference b-first)
                    (:difference inp)))
              b-rest)
        nil?
        not)))

(defn macd-crossover? [signals-macd]

  (let [fmacd (first signals-macd)
        nmacd (second signals-macd)]

    (and (< (:close-macd nmacd) (:ema-signal nmacd))
         (> (:close-macd fmacd ) (:ema-signal fmacd)))))

(defn macd-histogram-squeeze? [signals-macd]

  (let [h-first (first signals-macd)
        h-rest (take 2 (rest signals-macd))]

    (-> (some (fn [inp]
                (> (:histogram h-first)
                   (:histogram inp)))
              h-rest)
        nil?
        not)))

(defn macd-up-signal? [tick-list]

  (let [result (sleading/macd nil 20 tick-list)
        fresult (first result)

        some-ups? (some #(= :up (:signal %)) (:signals fresult))
        no-downs? (not-any? #(= :down (:signal %)) (:signals fresult))]

    (and some-ups? no-downs?)))


(defn obv-increasing? [signals-obv]

  (let [fst (first signals-obv)
        snd (second signals-obv)
        thd (nth signals-obv 2)]

    (or (> (:obv fst) (:obv snd))
        (> (:obv fst) (:obv thd)))))

(defn stochastic-oversold? [signals-stochastic]

  (-> (some (fn [inp]
              (<= (:K inp) 0.25))
            (take 2 signals-stochastic))
      nil?
      not))

(defn stochastic-crossover? [signals-stochastic]

  (let [fstochastic (first signals-stochastic)
        nstochastic (second signals-stochastic)]

    (and (< (:K nstochastic) (:D nstochastic))
         (> (:K fstochastic ) (:D fstochastic)))))


(defn strategy-A
  "This strategy is a composition of the below signals. It works only for the first tick.

   A. Price increase
   B. Price below the SMA

   C. Price was just at or below the bollinger-band (w/in last 2 ticks)
   D. Bollinger-Band was narrower (w/in last 2 ticks)

   E. MACD Histogram squeeze

   F. OBV increasing

   G. Stochastic is oversold, or was (w/in last 2 ticks)"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  (let [
        ;; *
        no-price-oscillationV (no-price-oscillation? tick-list)

        ;; * MACD Signal
        macd-up-signalV (macd-up-signal? tick-list)

        ;; A.
        price-increaseV (price-increase? tick-list)

        ;; B.
        price-below-smaV (price-below-sma? tick-list signals-ma)

        ;; C.
        bollinger-price-belowV (bollinger-price-below? tick-list signals-bollinger)

        ;; D.
        bollinger-was-narrowerV (bollinger-was-narrower? signals-bollinger)

        ;; E.
        macd-histogram-squeezeV (macd-histogram-squeeze? signals-macd)

        ;; F.
        obv-increasingV (obv-increasing? signals-obv)

        ;; G.
        stochastic-oversoldV (stochastic-oversold? signals-stochastic)]


    (if (and price-increaseV macd-up-signalV price-below-smaV bollinger-price-belowV bollinger-was-narrowerV macd-histogram-squeezeV obv-increasingV stochastic-oversoldV)

      ;; if all conditions are met, put an :up signal, with the reasons
      (do (println "BINGO ---- We have a strategy-A :up signal")
          (cons (assoc (first tick-list) :strategies [{:signal :up
                                                       :name :strategy-A
                                                       :why [:no-price-oscillation :macd-up-signal :price-increase :price-below-sma :bollinger-price-below
                                                             :bollinger-was-narrower :macd-histogram-squeeze :obv-increasing :stochastic-oversold]}])
                (rest tick-list))))))


(defn list-subset [key-list input-list]

  (->> (filter (fn [inp]
                 (some #{(:date inp)} key-list))

               input-list
               #_(remove (fn [inp]
                           (nil? (:date inp))) input-list))

       (sort-by :date)))


(defn strategy-fill-A
  "Applies strategy-A filters, for the entire length of the tick-list"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  (->> (reduce (fn [rslt ech-list]

                 (let [
                       key-list (into [] (flatten (map (fn [inp]
                                                         (vals (select-keys inp [:date])))
                                                       ech-list)))

                       ;; SUBSET of lists
                       ma-L (list-subset key-list signals-ma)
                       bollinger-L (list-subset key-list signals-bollinger)
                       macd-L (list-subset key-list signals-macd)
                       stochastic-L (list-subset key-list signals-stochastic)
                       obv-L (list-subset key-list signals-obv)]


                   ;; Make sure none of the lists are not empty
                   (if (or (< (count ma-L) 2)
                           (< (count bollinger-L) 3)
                           (< (count macd-L) 3)
                           (< (count stochastic-L) 3)
                           (< (count obv-L) 3))

                     rslt

                     (conj rslt (first (strategy-A ech-list ma-L bollinger-L macd-L stochastic-L obv-L))))))
               []
               (partition 10 1 tick-list))

       (remove nil?)))


(defn strategy-B
  "This strategy is a composition of the below signals. It works only for the first tick.

   *. Price increase

   A. Price crosses abouve SMA

   B. Bollinger-Band was narrower (w/in last 2 ticks)

   C. MACD crossover

   D. Stochastic crossover
   E. Stochastic is oversold or was (w/in last 2 ticks)

   F. OBV increasing"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  (let [

        ;; *
        no-price-oscillationV (no-price-oscillation? tick-list)

        ;; * MACD Signal
        macd-up-signalV (macd-up-signal? tick-list)

        ;; A.
        price-cross-abouve-smaV (price-cross-abouve-sma? tick-list signals-ma)

        ;; B.
        bollinger-was-narrowerV (bollinger-was-narrower? signals-bollinger)

        ;; C.
        macd-crossoverV (macd-crossover? signals-macd)

        ;; D.
        stochastic-crossoverV (stochastic-crossover? signals-stochastic)

        ;; E.
        stochastic-oversoldV (stochastic-oversold? signals-stochastic)

        ;; F.
        obv-increasingV (obv-increasing? signals-obv)]

    (if (and no-price-oscillationV macd-up-signalV price-cross-abouve-smaV bollinger-was-narrowerV macd-crossoverV stochastic-crossoverV stochastic-oversoldV obv-increasingV)

      (do (println "BINGO ---- We have a strategy-B :up signal")
          (cons (assoc (first tick-list) :strategies [{:signal :up
                                                       :name :strategy-B
                                                       :why [:price-increase :macd-up-signal :price-cross-abouve-sma :bollinger-was-narrower :macd-crossover
                                                             :stochastic-crossover :stochastic-oversold :obv-increasing]}])
                (rest tick-list))))))

(defn strategy-fill-B
  "Applies strategy-B filters, for the entire length of the tick-list"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  (->> (reduce (fn [rslt ech-list]

                 (let [
                       key-list (into [] (flatten (map (fn [inp]
                                                         (vals (select-keys inp [:date])))
                                                       ech-list)))

                       ;; SUBSET of lists
                       ma-L (list-subset key-list signals-ma)
                       bollinger-L (list-subset key-list signals-bollinger)
                       macd-L (list-subset key-list signals-macd)
                       stochastic-L (list-subset key-list signals-stochastic)
                       obv-L (list-subset key-list signals-obv)]

                   ;; Make sure the lists are not empty
                   (if (or (< (count ma-L) 2)
                           (< (count bollinger-L) 2)
                           (< (count macd-L) 2)
                           (empty? stochastic-L)
                           (< (count obv-L) 2))

                     rslt

                     (conj rslt (first (strategy-B ech-list ma-L bollinger-L macd-L stochastic-L obv-L))))))
               []
               (partition 10 1 tick-list))

       (remove nil?)))


(defn price-rising? [tick-list]

  (let [fst (first tick-list)
        snd (second tick-list)
        thd (nth tick-list 2)]

    (and (> (:close fst) (:close snd))
         (> (:close snd) (:close thd)))))

(defn price-rising-abouveMAs? [tick-list signals-ma]

  (let [simple-cross (price-cross-abouve-sma? tick-list signals-ma)
        exponential-cross (price-cross-abouve-sma? tick-list signals-ma {:compare-key :close-exponential})]

    (and simple-cross exponential-cross)))

(defn macd-histogram-rising? [signals-macd]

  (let [fst (first signals-macd)
        snd (second signals-macd)
        thd (nth signals-macd 2)]

    (and (> (:histogram fst) (:histogram snd))
         (> (:histogram snd) (:histogram thd)))))

(defn obv-rising? [signals-obv]

  (let [fst (first signals-obv)
        snd (second signals-obv)
        thd (nth signals-obv 2)]

    (and (> (:obv fst) (:obv snd))
         (> (:obv snd) (:obv thd)))))


(defn strategy-C
  "I want to be able to catch a run.

   A. price greater than previous ; previous greater than next previous
   B. price crossed abouve SMA and EMA w/ in last 3 ticks

   C. MACD Histogram greater than previous ; previous greater than next previous

   D. OBV greater than previous ; previous greater than next previous"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  (let [
        ;; A.
        price-risingV (price-rising? tick-list)

        ;; B.
        price-rising-abouveMAsV (price-rising-abouveMAs? tick-list signals-ma)

        ;; C.
        macd-histogram-risingV (macd-histogram-rising? signals-macd)

        ;; D.
        obv-risingV (obv-rising? signals-obv)
        ]

    (if (and price-risingV price-rising-abouveMAsV macd-histogram-risingV obv-risingV)

      (do (println "BINGO ---- We have a strategy-C :up signal")
          (cons (assoc (first tick-list) :strategies [{:signal :up
                                                       :name :strategy-C
                                                       :why [:price-increase :macd-up-signal :price-cross-abouve-sma :bollinger-was-narrower :macd-crossover
                                                             :stochastic-crossover :stochastic-oversold :obv-increasing]}])
                (rest tick-list))))))
