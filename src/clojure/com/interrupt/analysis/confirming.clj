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

                           (if-let [prev-obv (:obv (last rslt))]    ;; handling case where first will not have an OBV

                             ;; normal case
                             (let [current-price (:close (last ech))
                                   prev-price (:close (last (butlast ech)))
                                   current-volume (:volume (last ech))

                                   obv (if (= current-price prev-price)
                                         prev-obv
                                         (if (> current-price prev-price)
                                           (+ prev-obv current-volume)
                                           (- prev-obv current-volume)))]

                               (concat (into [] rslt)
                                       [{:obv obv
                                         :volume (:volume (last ech))
                                         :close (:close (last ech))
                                         :date (:date (last ech))}]))

                             ;; otherwise we seed the list with the first entry
                             (concat (into [] rslt)
                                     [{:obv (:volume (last ech))
                                       :volume (:volume (last ech))
                                       :close (:close (last ech))
                                       :date (:date (last ech))}])))
                         '(nil)
                         (partition 2 1 tick-list))]

    ;; calculate OBV for latest tick
    (if latest-tick

      (let [cprice (:close latest-tick)
            pprice (:close (last obv-list))
            cvolume (:volume latest-tick)
            pobv (:obv (last obv-list))

            cobv (if (= cprice pprice)
                   pobv
                   (if (> cprice pprice)
                     (+ pobv cvolume)
                     (- pobv cvolume)))]

        (concat (into [] obv-list)
                [{:obv cobv
                  :volume (:volume latest-tick)
                  :close (:close latest-tick)
                  :date (:date latest-tick)}]))
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

                                       (let [fst (:close (last ech))
                                             snd (:close (second ech))

                                             up? (> fst snd)
                                             down? (< fst snd)
                                             sideways? (and (not up?) (not down?))]

                                         (if (or up? down?)
                                           (if up?
                                             (concat (into [] rslt) [(assoc (last ech) :signal :up)])
                                             (concat (into [] rslt) [(assoc (last ech) :signal :down)]))
                                           (concat (into [] rslt) [(assoc (last ech) :signal :sideways)]))))
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

                (concat (into [] rslt)
                        [{:date (:date (last ech))
                          :close (:close (last ech))
                          :rs rs
                          :rsi rsi}])))
            []
            window-list)))
