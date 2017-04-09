(ns com.interrupt.edgarly.core
  (:require  [com.stuartsierra.component :as component]
             [system.repl :refer [set-init! init start stop reset refresh system]]
             [system.components.repl-server :refer [new-repl-server]]
             [com.interrupt.component.ewrapper :refer [new-ewrapper]]
             [clojure.core.async :refer [chan >! <! merge go go-loop pub sub unsub-all sliding-buffer]]
             [clojure.core.match :refer [match]]
             [clojure.spec :as s]
             [clojure.spec.gen :as sg]
             [clojure.spec.test :as st]
             [clojure.future :refer :all]
             [clojure.set :as cs]
             [com.interrupt.component.ewrapper-impl :as ei]
             [clojure.pprint :refer [pprint]]
             [com.rpl.specter :refer [transform select ALL]]
             )
  (:import [java.util.concurrent TimeUnit]))


(def config
  {:stocks {:default-instrument "STK"
            :default-location "STK.US.MAJOR"}

   :scanners [{:key :high-opt-imp-volat
               :scan-name "HIGH_OPT_IMP_VOLAT"
               :tag :volatility}]

   #_:scanners #_[{:scan-name "HIGH_OPT_IMP_VOLAT"
                   :scan-value {}
                   :tag :volatility}
                  {:scan-name "HIGH_OPT_IMP_VOLAT_OVER_HIST"
                   :scan-value {}
                   :tag :volatility}
                  {:scan-name "HOT_BY_VOLUME"
                   :scan-value {}
                   :tag :volume}
                  {:scan-name "TOP_VOLUME_RATE"
                   :scan-value {}
                   :tag :volume}
                  {:scan-name "HOT_BY_OPT_VOLUME"
                   :scan-value {}
                   :tag :volume}
                  {:scan-name "OPT_VOLUME_MOST_ACTIVE"
                   :scan-value {}
                   :tag :volume}
                  {:scan-name "COMBO_MOST_ACTIVE"
                   :scan-value {}
                   :tag :volume}
                  {:scan-name "MOST_ACTIVE_USD"
                   :scan-value {}
                   :tag :price}
                  {:scan-name "HOT_BY_PRICE"
                   :scan-value {}
                   :tag :price}
                  {:scan-name "TOP_PRICE_RANGE"
                   :scan-value {}
                   :tag :price}
                  {:scan-name "HOT_BY_PRICE_RANGE"
                   :scan-value {}
                   :tag :price}]})

(defn system-map []
  (component/system-map
   :nrepl (new-repl-server 7888 "0.0.0.0")  ;; useful when operating to the cloud
   :ewrapper (new-ewrapper)))

(set-init! #'system-map)

(defn start-system [] (start))
(defn stop-system [] (stop))

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

(defn scanner-subscriptions-with-ids [confg scanner-subscriptions]

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

(defn consume-subscriber [scanner-subscriptions-atom subscriber-chan]
  (go-loop [r1 nil]

    (let [{:keys [req-id symbol rank] :as val} (select-keys r1 [:req-id :symbol :rank])]
      (if (and r1 rank)
        (swap! scanner-subscriptions-atom
               (fn [scans]
                 (transform [ALL #(= (::reqid %) req-id) ::scan-value]
                            #(assoc % rank val)
                            scans)))))

    (recur (<! subscriber-chan))))

(defn scanner-start [client publication config]

  (let [default-instrument (-> config :stocks :default-instrument)
        default-location (-> config :stocks :default-location)
        scanner-subscriptions []
        scanner-subscriptions-atom (atom (scanner-subscriptions-with-ids config scanner-subscriptions))]

    (doseq [{:keys [::reqid ::scan-name ::tag] :as val} @scanner-subscriptions-atom
            :let [subscriber (chan)]]

      (ei/scanner-subscribe reqid client default-instrument default-location scan-name)
      (sub publication reqid subscriber)
      (consume-subscriber scanner-subscriptions-atom subscriber))

    scanner-subscriptions-atom))

(comment

  (def client (-> system.repl/system :ewrapper :ewrapper :client))
  (def publisher (-> system.repl/system :ewrapper :ewrapper :publisher))
  (def publication
    (pub publisher #(:req-id %)))

  (def scanner-subscriptions (scanner-start client publication config))

  (ei/scanner-unsubscribe 1 client)

  (def one [{:com.interrupt.edgarly.core/reqid 1
             :com.interrupt.edgarly.core/scan-name "HIGH_OPT_IMP_VOLAT"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :volatility}
            {:com.interrupt.edgarly.core/reqid 2
             :com.interrupt.edgarly.core/scan-name "HIGH_OPT_IMP_VOLAT_OVER_HIST"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :volatility}
            {:com.interrupt.edgarly.core/reqid 3
             :com.interrupt.edgarly.core/scan-name "HOT_BY_VOLUME"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :volume}
            {:com.interrupt.edgarly.core/reqid 4
             :com.interrupt.edgarly.core/scan-name "TOP_VOLUME_RATE"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :volume}
            {:com.interrupt.edgarly.core/reqid 5
             :com.interrupt.edgarly.core/scan-name "HOT_BY_OPT_VOLUME"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :volume}
            {:com.interrupt.edgarly.core/reqid 6
             :com.interrupt.edgarly.core/scan-name "OPT_VOLUME_MOST_ACTIVE"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :volume}
            {:com.interrupt.edgarly.core/reqid 7
             :com.interrupt.edgarly.core/scan-name "COMBO_MOST_ACTIVE"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :volume}
            {:com.interrupt.edgarly.core/reqid 8
             :com.interrupt.edgarly.core/scan-name "MOST_ACTIVE_USD"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :price}
            {:com.interrupt.edgarly.core/reqid 9
             :com.interrupt.edgarly.core/scan-name "HOT_BY_PRICE"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :price}
            {:com.interrupt.edgarly.core/reqid 10
             :com.interrupt.edgarly.core/scan-name "TOP_PRICE_RANGE"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :price}
            {:com.interrupt.edgarly.core/reqid 11
             :com.interrupt.edgarly.core/scan-name "HOT_BY_PRICE_RANGE"
             :com.interrupt.edgarly.core/scan-value {}
             :com.interrupt.edgarly.core/tag :price}])

  )


(defn scanner-stop [])


(defn historical-start [])
(defn historical-stop [])

(defn market-start [])
(defn market-stop [])

(defn open-request-ids [])

(defn -main [& args]
  (Thread/sleep 000) ;; a hack, to ensure that the tws machine is available, before we try to connect to it.
  (start-system))

