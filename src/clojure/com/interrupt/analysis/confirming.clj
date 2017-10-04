(ns com.interrupt.analysis.confirming)


(defn on-balance-volume
  "On Balance Volume (OBV) measures buying and selling pressure as a cumulative indicator that i) adds volume on up days and ii) subtracts volume on down days. We'll look for divergences between OBV and price to predict price movements or use OBV to confirm price trends.

   The On Balance Volume (OBV) line is a running total of positive and negative volume. i) A tick's volume is positive when the close is above the prior close. Or ii) a tick's volume is negative when the close is below the prior close.

    If closing price is above prior:
      Current OBV = Previous OBV + Current Volume

    If closing price is below prior:
      Current OBV = Previous OBV  -  Current Volume

    If closing price equals prior:
      Current OBV = Previous OBV (no change)

    ** The first OBV value is the first period's positive/negative volume.
    ** This function assumes the latest tick is on the left**"
  [latest-tick tick-list]

  ;; accumulate OBV on historical tick-list
  (let [obv-list (reduce (fn [rslt ech]

                           (if-let [prev-obv (:obv (first rslt))]    ;; handling case where first will not have an OBV

                             ;; normal case
                             (let [current-price (if (string? (:close (first ech)))
                                                   (read-string (:close (first ech)))
                                                   (:close (first ech)))
                                   prev-price (if (string? (:close (second ech)))
                                                (read-string (:close (second ech)))
                                                (:close (second ech)))
                                   current-volume (if (string? (:total-volume (first ech)))
                                                    (read-string (:total-volume (first ech)))
                                                    (:total-volume (first ech)))

                                   obv (if (= current-price prev-price)
                                         prev-obv
                                         (if (> current-price prev-price)
                                           (+ prev-obv current-volume)
                                           (- prev-obv current-volume)))]

                               (cons {:obv obv
                                      :total-volume (:total-volume (first ech))
                                      :close (:close (first ech))
                                      :date (:date (first ech))} rslt))

                             ;; otherwise we seed the list with the first entry
                             (cons {:obv (:total-volume (first ech))
                                    :total-volume (:total-volume (first ech))
                                    :close (:close (first ech))
                                    :date (:date (first ech))} rslt)))
                         '(nil)
                         (->> tick-list (partition 2 1) reverse))]

    ;; calculate OBV for latest tick
    (if latest-tick

      (let [cprice (if (string? (:close latest-tick))
                     (read-string (:close latest-tick))
                     (:close latest-tick))
            pprice (if (string? (:close (first obv-list)))
                     (read-string (:close (first obv-list)))
                     (:close (first obv-list)))
            cvolume (if (string? (:total-volume latest-tick))
                      (read-string (:total-volume latest-tick))
                      (:total-volume latest-tick))
            pobv (:obv (first obv-list))

            cobv (if (= cprice pprice)
                   pobv
                   (if (> cprice pprice)
                     (+ pobv cvolume)
                     (- pobv cvolume)))]

        (cons {:obv cobv
               :total-volume (:total-volume latest-tick)
               :close (:close latest-tick)
               :date (:date latest-tick)} obv-list))
      obv-list)))

(defn relative-strength-index
  "The Relative Strength Index (RSI) is a momentum oscillator that measures the speed and change of price movements. It oscillates between zero and 100.

   If no 'tick-window' is given, it defaults to 14

   ** This function assumes the latest tick is on the left**"
  [tick-window tick-list]

  (let [twindow (if tick-window tick-window 14)
        window-list (partition twindow 1 tick-list)]

    ;; run over the collection of populations
    (reduce (fn [rslt ech]

              ;; each item will be a population of tick-window (default of 14)
              (let [pass-one (reduce (fn [rslt ech]

                                       (let [fst (:close (first ech))
                                             snd (:close (second ech))

                                             up? (> fst snd)
                                             down? (< fst snd)
                                             sideways? (and (not up?) (not down?))]

                                         (if (or up? down?)
                                           (if up?
                                             (conj rslt (assoc (first ech) :signal :up))
                                             (conj rslt (assoc (first ech) :signal :down)))
                                           (conj rslt (assoc (first ech) :signal :sideways)))))
                                     []
                                     (partition 2 1 (remove nil? ech)))


                    up-list (:up (group-by :signal pass-one))
                    down-list (:down (group-by :signal pass-one))

                    avg-gains (/ (apply +
                                        (map :close up-list))
                                 tick-window)
                    avg-losses (/ (apply +
                                         (map :close down-list))
                                  tick-window)

                    rs (if-not (= 0 avg-losses)
                         (/ avg-gains avg-losses)
                         0)
                    rsi (- 100 (/ 100 (+ 1 rs)))]

                (conj rslt {:date (:date (first ech))
                            :close (:close (first ech))
                            :rs rs
                            :rsi rsi})))
            []
            window-list)))
