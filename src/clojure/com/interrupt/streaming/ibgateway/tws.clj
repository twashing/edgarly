(ns com.interrupt.streaming.ibgateway.tws
  (:require [clojure.spec.alpha :as s]
            [clojure.future :refer [pos-int?]]
            [clojure.core.async :refer [chan pub sub go-loop >!! <!! >! <!]]
            [clojure.set :as cs]
            [clojure.string :as str]
            [clojure.core.match :refer [match]]
            [system.repl]

            [com.interrupt.streaming.platform.base :as base]
            [com.interrupt.streaming.platform.serialization]
            [com.interrupt.component.ewrapper-impl :as ei]))

(s/def ::reqid pos-int?)
(s/def ::subscription-element (s/keys :req [::reqid]))
(s/def ::subscriptions (s/coll-of ::subscription-element))

(defn scannerid-availableid-pairs [scanner-subscriptions]
  (let [scannerids (sort (map ::reqid scanner-subscriptions))
        scannerids-largest (last scannerids)
        first-id (first scannerids)
        contiguous-numbers (take 10 (range 1 scannerids-largest))
        availableids (sort (cs/difference (into #{} contiguous-numbers)
                                          (into #{} scannerids)))]

    [scannerids availableids]))

(defn next-reqid [scanner-subscriptions]
  (match [scanner-subscriptions]
         [nil] 1
         [[]] 1
         :else (let [[scannerids availableids] (scannerid-availableid-pairs scanner-subscriptions)]
                 (if-not (empty? availableids)
                   (first availableids)
                   (+ 1 (last scannerids))))))

(s/fdef next-reqid
        :args (s/cat :subscriptions ::subscriptions)
        :ret number?
        :fn (s/and

             ;; Handles nil and empty sets
             #(if (empty? (-> % :args :subscriptions))
                (= 1 (:ret %))
                (pos-int? (:ret %)))

             ;; Finds the first gap number
             ;; Can be in first position
             ;; Gap can be on left or right side
             (fn [x]
               (let [reqids (sort (map ::reqid (-> x :args :subscriptions)))
                     fid (first reqids)]
                 (match [fid]
                        [nil] 1
                        [(_ :guard #(> % 1))] (= 1 (:ret x))
                        :else (pos-int? (:ret x)))))))

(defn top-level-scan-item [scan-name]
  (let [scan-sym #_spy/d (-> scan-name (str/lower-case) (str/replace "_" "-") symbol)]
    (if-let [scan-resolved (resolve scan-sym)]
      scan-resolved
      (intern *ns* scan-sym (atom {})))))

(defn scanner-subscriptions-with-ids [config scanner-subscriptions]

  (let [scan-types (->> config :scanners (map #(select-keys % [:scan-name :tag])))]

    (reduce (fn [acc {:keys [scan-name tag]}]
              (let [next-id (next-reqid acc)
                    subscription {::reqid next-id
                                  ::scan-name scan-name
                                  ::scan-value {}
                                  ::tag tag}]
                (conj acc subscription)))
            scanner-subscriptions
            scan-types)))

(defn consume-subscriber [scan-atom subscriber-chan]
  (go-loop [r1 nil]
    (let [{:keys [req-id symbol rank] :as val} (select-keys r1 [:req-id :symbol :rank])]
      (if (and r1 rank)
        (swap! scan-atom assoc rank val)))

    (recur (<! subscriber-chan))))

(defn scanner-start [client publication config]

  (let [default-instrument (-> config :stocks :default-instrument)
        default-location (-> config :stocks :default-location)
        scanner-subscriptions-init []
        scanner-subscriptions (scanner-subscriptions-with-ids config scanner-subscriptions-init)]

    (doseq [{:keys [::reqid ::scan-name ::tag] :as val} scanner-subscriptions
            :let [subscriber (chan)]]

      (let [scan-var (top-level-scan-item scan-name)
            scan-atom (var-get scan-var)]
        (ei/scanner-subscribe reqid client default-instrument default-location scan-name)
        (sub publication reqid subscriber)
        (consume-subscriber scan-atom subscriber)))

    scanner-subscriptions))

(defn scanner-stop [])
