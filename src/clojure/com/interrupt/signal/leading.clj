(ns com.interrupt.signal.leading
  (:require [com.interrupt.analysis.leading :as lead-analysis]
            [com.interrupt.analysis.lagging :as lag-analysis]
            [com.interrupt.signal.common :as common]))


(defn macd-cross-abouve?
  "** This function assumes the latest tick is on the left**"
  [fst snd]
  (and (< (:close-macd snd) (:ema-signal snd))
       (> (:close-macd fst) (:ema-signal snd))))

(defn macd-cross-below?
  "** This function assumes the latest tick is on the left**"
  [fst snd]
  (and (> (:close-macd snd) (:ema-signal snd))
       (< (:close-macd fst) (:ema-signal fst))))

(defn macd-signal-crossover
  "** This function assumes the latest tick is on the left**"
  [macd-list]

  (let [partitioned-list (partition 2 1 (remove nil? macd-list))]

    (reduce (fn [rslt ech]

              (let [fst (first ech)
                    snd (second ech)

                    macd-cross-A? (macd-cross-abouve? fst snd)
                    macd-cross-B? (macd-cross-below? fst snd)]

                (if (or macd-cross-A? macd-cross-B?)

                  (if macd-cross-A?
                    (conj rslt (assoc fst :signals [{:signal :up
                                                     :why :macd-signal-crossover
                                                     :arguments [ech]
                                                     #_:function #_macd-cross-abouve?}]))
                    (conj rslt (assoc fst :signals [{:signal :down
                                                     :why :macd-signal-crossover
                                                     :arguments [ech]
                                                     #_:function #_macd-cross-below?}])))
                  (conj rslt fst))))
            []
            partitioned-list)))

(defn macd-divergence
  "** This function assumes the latest tick is on the left**"
  [view-window macd-list]

  (let [partitioned-macd (partition view-window 1 macd-list)


        ;; B i.
        ;;    when i. closing price makes a higher high and ii. MACD makes a lower high
        ;;    ... TODO - when price rises and falls quickly
        divergence-macd (reduce (fn [rslt ech-list]

                                  (let [fst (first ech-list)

                                        price-peaks-valleys (common/find-peaks-valleys nil ech-list)
                                        macd-peaks-valleys (common/find-peaks-valleys {:input :close-macd} ech-list)

                                        dUP? (common/divergence-up? nil  ech-list price-peaks-valleys macd-peaks-valleys)
                                        dDOWN? (common/divergence-down? nil ech-list price-peaks-valleys macd-peaks-valleys)
                                        ]

                                    (if (or dUP? dDOWN?)

                                      (if dUP?
                                        (conj rslt (assoc fst :signals [{:signal :up
                                                                         :why :macd-divergence
                                                                         :arguments [ech-list price-peaks-valleys macd-peaks-valleys]
                                                                         #_:function #_common/divergence-up?}]))
                                        (conj rslt (assoc fst :signals [{:signal :down
                                                                         :why :macd-divergence
                                                                         :arguments [ech-list price-peaks-valleys macd-peaks-valleys]
                                                                         #_:function #_common/divergence-down?}])))
                                      (conj rslt (first ech-list)))))
                                []
                                partitioned-macd)

        ;; B ii. ... TODO - if histogram goes into negative territory

        ]

    divergence-macd))

(defn macd
  "This functions searches for signals to overlay on top of a regular MACD time series. It uses the following strategies

   A. MACD / signal crossover
      when i. MACD line crosses over the ii. signal line

   B. MACD divergence

      i)
      when i. closing price makes a higher high and ii. MACD makes a lower high
      ... TODO - when price rises and falls quickly

      OR

      ii)
      look for high price resistance over last 3 peaks
      when i. closing price makes a higher high and ii. histogram makes a lower high

      ... TODO - after both are true, look for
         i. subsequent 3 closing prices to be below the high

         OR

         ii. if histogram goes into negative territory

   ... TODO - C. MACD Stairsteps (http://www.youtube.com/watch?v=L-cB_zZcpks)

      ENTRY:
         when i. MACD crosses over ii. the signal line
         when subsequent 3 low(s) are equal or greater than the previous high(s)

      EXIT:
         measure last up-move and project target (difference from last high, from low); stop below the current low.

   ** This function assumes the latest tick is on the left**"

  ([options tick-window tick-list]
   (macd options tick-window tick-list (lag-analysis/simple-moving-average nil tick-window tick-list)))

  ([options tick-window tick-list sma-list]
   (let [macd-list (lead-analysis/macd options tick-window tick-list sma-list)]
     (macd options tick-window tick-list sma-list macd-list)))

  ([options tick-window tick-list sma-list macd-list]


   (let [

         ;; A.
         macd-A (macd-signal-crossover macd-list)


         ;; B.
         macd-B (macd-divergence 10 macd-list)

         ;; C.
         ]

     ;; joining the results of all the signals
     (map (fn [e1 e2]

            (if (some #(not (nil? (:signals %))) [e1 e2])
              (assoc e1 :signals (concat (:signals e1)
                                         (:signals e2)))
              e1))
          macd-A
          macd-B))))

(defn is-overbought? [level ech]
  (> (:K ech) level))

(defn is-oversold? [level ech]
  (< (:K ech) level))

(defn stochastic-level
  "** This function assumes the latest tick is on the left**"
  [stochastic-list]
  (reduce (fn [rslt ech]

            (let [
                  OVERBOUGHT 0.8
                  OVERSOLD 0.2

                  isOB? (is-overbought? OVERBOUGHT ech)
                  isOS? (is-oversold? OVERSOLD ech)]

              (if (or isOB? isOS?)
                (if isOB?
                  (conj rslt (assoc ech :signals [{:signal :down
                                                   :why :stochastic-overbought
                                                   :arguments [OVERBOUGHT ech]
                                                   #_:function #_is-overbought?}]))
                  (conj rslt (assoc ech :signals [{:signal :up
                                                   :why :stochastic-oversold
                                                   :arguments [OVERSOLD ech]
                                                   #_:function #_is-oversold?}])))
                (conj rslt ech))))
          []
          (remove nil? stochastic-list)))

(defn k-crosses-abouve?
  "** This function assumes the latest tick is on the left**"
  [fst snd]
  (and (< (:K snd) (:D snd))
       (> (:K fst) (:D fst))))

(defn k-crosses-below?
  "** This function assumes the latest tick is on the left**"
  [fst snd]
  (and (> (:K snd) (:D snd))
       (< (:K fst) (:D fst))))

(defn stochastic-crossover
  "** This function assumes the latest tick is on the left**"
  [partitioned-stochastic]

  (reduce (fn [rslt ech]

            (let [fst (first ech)
                  snd (second ech)

                  both-exist? (and (-> fst nil? not)
                                   (-> snd nil? not))

                  kA? (and both-exist?
                           (k-crosses-abouve? fst snd))
                  kB? (and both-exist?
                           (k-crosses-below? fst snd))]

              (if (or kA? kB?)
                (if kA?
                  (conj rslt (assoc fst :signals [{:signal :down
                                                   :why :stochastic-crossover
                                                   :arguments [fst snd]
                                                   #_:function #_k-crosses-abouve?}]))
                  (conj rslt (assoc fst :signals [{:signal :up
                                                   :why :stochastic-crossover
                                                   :arguments [fst snd]
                                                   #_:function #_k-crosses-below?}])))
                (conj rslt fst))))
          []
          partitioned-stochastic))

(defn stochastic-divergence
  "** This function assumes the latest tick is on the left**"
  [view-window stochastic-list]

  (let [partitioned-stochastic (partition view-window 1 stochastic-list)


        ;; when i. closing price makes a higher high and ii. MACD makes a lower high
        divergence-stochastic (reduce (fn [rslt ech-list]

                                        (let [fst (first ech-list)

                                              k-peaks-valleys (common/find-peaks-valleys {:input :K} ech-list)
                                              d-peaks-valleys (common/find-peaks-valleys {:input :D} ech-list)

                                              dUP? (common/divergence-up? {:input-top :K :input-bottom :D} ech-list k-peaks-valleys d-peaks-valleys)
                                              dDOWN? (common/divergence-down? {:input-top :K :input-bottom :D} ech-list k-peaks-valleys d-peaks-valleys)]

                                          (if (or dUP? dDOWN?)

                                            (if dUP?
                                              (conj rslt (assoc fst :signals [{:signal :up
                                                                               :why :stochastic-divergence
                                                                               :arguments [ech-list k-peaks-valleys d-peaks-valleys]
                                                                               #_:function #_common/divergence-up?}]))
                                              (conj rslt (assoc fst :signals [{:signal :down
                                                                               :why :stochastic-divergence
                                                                               :arguments [ech-list k-peaks-valleys d-peaks-valleys]
                                                                               #_:function #_common/divergence-down?}])))
                                            (conj rslt (first ech-list)))))
                                      []
                                      partitioned-stochastic)]

    divergence-stochastic))

(defn stochastic-oscillator
  "This function searches for signals to overlay on top of a regular Stochastic Oscillator time series.

   A. Look for the %K Stochastic line to be abouve (0.8) or below (0.2) the overbought and oversold levels, respectively
   B. Look for %K Stochastic line to cross over the %D trigger line
   C. Look for Divergence, where i. price makes a higher high AND %K Stochastic makes a lower low.

   ** This function assumes the latest tick is on the left**"

  ([tick-window trigger-window trigger-line tick-list]
   (let [stochastic-list (lead-analysis/stochastic-oscillator tick-window trigger-window trigger-line tick-list)]
     (stochastic-oscillator tick-window trigger-window trigger-line tick-list stochastic-list)))

  ([tick-window trigger-window trigger-line tick-list stochastic-list]

   (let [
         ;; A. is %K abouve or below the overbought or oversold levels
         stochastic-A (stochastic-level stochastic-list)


         ;; B. Does %K Stochastic line cross over the %D trigger line
         stochastic-B (stochastic-crossover (partition 2 1 stochastic-list))


         ;; C. Look for Divergence, where i. price makes a higher high AND %K Stochastic makes a lower low
         stochastic-C (stochastic-divergence 10 stochastic-list)]


     ;; joining the results of all the signals
     (map (fn [e1 e2 e3]

            (if (some #(not (nil? (:signals %))) [e1 e2 e3])
              (assoc e1 :signals (concat (:signals e1)
                                         (:signals e2)
                                         (:signals e3)))
              e1))
          stochastic-A
          stochastic-B
          stochastic-C))))
