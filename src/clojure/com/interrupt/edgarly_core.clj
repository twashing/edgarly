(ns com.interrupt.edgarly-core)

(defn hello
  "Say hello to caller"
  [caller]
  (format "Hello, %s!" caller))


#_(ns edgar.ib.market
    (:import [com.ib.client EWrapper EClientSocket Contract Order OrderState ContractDetails Execution])
    (:use [clojure.core.strint])
    (:require [edgar.eclientsocket :as socket]
              [lamina.core :as lamina]
              [overtone.at-at :as at]
              [clj-time.core :as cime]
              [clj-time.local :as time]
              [clj-time.format :as format]))


#_(defn connect-to-market
    "Connect to the IB marketplace. This should return a 'client' object"
    []
    (socket/connect-to-tws))

#_(defn disconnect-from-market
    "Disconnect from the IB marketplace."
    []
    (socket/disconnect-from-tws))
