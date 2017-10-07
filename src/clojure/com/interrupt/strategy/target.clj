(ns edgar.core.strategy.target)


(defn percentage-change [orig-price current-price]

  (let [change-prc (- current-price orig-price)
        change-pct (/ change-prc orig-price)]

    change-pct))



;; Making a default stop / loss threshold of -5%
(defn stoploss-threshhold? [orig-price current-price]

  (let [change-pct (percentage-change orig-price current-price)
        change-test (> -0.05 change-pct)]

    change-test))

(defn stoploss-threshhold-wstocks? [orig-price current-price no-shares]

  (let [balance-org (* orig-price no-shares)
        balance-crt (* current-price no-shares)
        balance-trg (+ balance-org (* balance-org -0.05))
        change-test (< balance-crt balance-trg)]

    change-test))




;; Making a default target threshold of 5%
(defn target-threshhold? [orig-price current-price]

  (let [change-pct (percentage-change orig-price current-price)
        change-test (>= change-pct 0.05)]

    change-test))

(defn target-threshhold-wstocks? [orig-price current-price no-shares]

  (let [balance-org (* orig-price no-shares)
        balance-crt (* current-price no-shares)
        balance-trg (* balance-org 1.05)
        change-test (>= balance-crt balance-trg)]

    change-test))
