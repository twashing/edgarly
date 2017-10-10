(ns com.interrupt.signal.common)


(defn find-peaks-valleys
  "** This function assumes the latest tick is on the right**"
  [options tick-list]

  (let [{input-key :input
         :or {input-key :close}} options]

    (reduce (fn [rslt ech]
              (let [fst (input-key (first ech))

                    snd (input-key (second ech))

                    thd (input-key (nth ech 2))

                    valley? (and (and (-> fst nil? not) (-> snd nil? not) (-> thd nil? not))
                                 (> fst snd)
                                 (< snd thd))

                    peak? (and (and (-> fst nil? not) (-> snd nil? not) (-> thd nil? not))
                               (< fst snd)
                               (> snd thd))]

                (if (or valley? peak?)
                  (if peak?
                    (concat (into [] rslt) [(assoc (second ech) :signal :peak)])
                    (concat (into [] rslt) [(assoc (second ech) :signal :valley)]))
                  rslt)))
            []
            (partition 3 1 tick-list))))

(defn up-market?
  "** This function assumes the latest tick is on the right **"
  [period partitioned-list]
  (every? (fn [inp]
            (> (:close (second inp))
               (:close (first inp))))
          (take period partitioned-list)))

(defn down-market?
  "** This function assumes the latest tick is on the right **"
  [period partitioned-list]
  (every? (fn [inp]
            (< (:close (second inp))
               (:close (first inp))))
          (take period partitioned-list)))

(defn divergence-up?
  "** This function assumes the latest tick is on the right **"
  [options ech-list price-peaks-valleys macd-peaks-valleys]

  (let [first-ech (first ech-list)
        first-price (first price-peaks-valleys)
        first-macd (first macd-peaks-valleys)

        {input-top :input-top
         input-bottom :input-bottom
         :or {input-top :close
              input-bottom :close-macd}} options


        both-exist-price? (and (not (empty? ech-list))
                                      (not (empty? price-peaks-valleys)))
        price-higher-high? (and (-> (input-top first-ech) nil? not)
                                (-> (input-top first-price) nil? not)
                                both-exist-price?
                                (> (input-top first-ech)
                                   (input-top first-price)))

        both-exist-macd? (and (not (empty? ech-list))
                              (not (empty? macd-peaks-valleys)))
        macd-lower-high? (and (-> (input-bottom first-ech) nil? not)
                              (-> (input-bottom first-macd) nil? not)
                              both-exist-macd?
                              (< (input-bottom first-ech)
                                 (input-bottom first-macd)))]

    (and price-higher-high? macd-lower-high?)))

(defn divergence-down?
  "** This function assumes the latest tick is on the left**"
  [options ech-list price-peaks-valleys macd-peaks-valleys]

  (let [
        first-ech (first ech-list)
        first-price (first price-peaks-valleys)
        first-macd (first macd-peaks-valleys)

        {input-top :input-top
         input-bottom :input-bottom
         :or {input-top :close
              input-bottom :close-macd}} options


        both-exist-price? (and (not (empty? (remove nil? ech-list)))
                               (not (empty? (remove nil? price-peaks-valleys))))
        price-lower-high? (and (-> (input-top first-ech) nil? not)
                               (-> (input-top first-price) nil? not)

                               both-exist-price?
                               (< (input-top (first ech-list)) (input-top (first price-peaks-valleys))))

        both-exist-macd? (and (not (empty? (remove nil? ech-list)))
                              (not (empty? (remove nil? macd-peaks-valleys))))
        macd-higher-high? (and (-> (input-top first-ech) nil? not)
                               (-> (input-top first-price) nil? not)

                               both-exist-macd?
                               (> (input-bottom (first ech-list)) (input-bottom (first macd-peaks-valleys))))]

    (and price-lower-high? macd-higher-high?)))
