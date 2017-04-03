(ns com.interrupt.streaming.customer-account-adapter
  "Customer account adapter."
  (:require [clj-uuid :as uuid]
            [clojure.tools.logging :as log]
            [finops.topics :as topics]
            [java-time :as time]
            [kafka.streams :as k]))

(defn investor-portal-investor->customer-account
  "Transforms an investor-portal-investor payload into a customer-account payload"
  [[k {:keys [payload] :as msg}]]
  (log/info "Mapping investor->customer" {:msg msg})
  (let [customer-account-id (:id payload)]
    [customer-account-id
     (when customer-account-id
       {:id customer-account-id
        :geography :usa
        :description (:name payload)
        :tracking-id (str (uuid/v4))
        :published-by "finops/customer-account-adapter"
        :published-at (time/to-millis-from-epoch (time/instant))})]))

(defn topology
  "Sets up the customer-account-adapter stream."
  [builder & _]
  (-> builder
      (k/kstream topics/investor-portal-investors)
      (k/map investor-portal-investor->customer-account)
      (k/to! topics/customer-account)))
