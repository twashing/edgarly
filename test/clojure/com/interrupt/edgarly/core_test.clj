(ns clojure.com.interrupt.edgarly.core-test
  (:require [clojure.test :as t]
            [clojure.spec :as s]
            [clojure.spec.test :as st]
            [com.interrupt.edgarly.core :as sut]
            [com.interrupt.test :refer [defspec-test] :as it]))


#_(defspec-test test-core `sut/next-reqid)

(clojure.spec.test/check `sut/next-reqid)


(def scanner-subscriptions-a [{::name "one"
                               ::val {}
                               ::tag :volatility
                               ::reqid 1}
                              {::name "three"
                               ::val {}
                               ::tag :volume
                               ::reqid 3}
                              {::name "eight"
                               ::val {}
                               ::tag :price
                               ::reqid 8}])

(def scanner-subscriptions-b [{::name "one"
                               ::val {}
                               ::tag :volatility
                               ::reqid 2}
                              {::name "three"
                               ::val {}
                               ::tag :volume
                               ::reqid 3}
                              {::name "eight"
                               ::val {}
                               ::tag :price
                               ::reqid 8}])

(scannerid-availableid-pairs scanner-subscriptions-a)
(scannerid-availableid-pairs scanner-subscriptions-b)

(comment

  (sut/next-reqid scanner-subscriptions-a)
  (sut/next-reqid scanner-subscriptions-b)

  (def one (s/exercise-fn `sut/next-reqid))
  (def two (st/check `sut/next-reqid)))

;; compares existing list of request IDs, to the set of numbers from 1 to the highest number
;;
;; (3 5 15) -> 1 should be the first gap
;; (1 2 3 4 5 6 7 8 9 10 11 12 13 14)
;;
;; (1 2 3 5) -> 4 should be the first gap
;; (1 2 3 4 5)
;;
;; (1 2 3 4 5 6 7 8 9 10)
;; (1 2 10) -> 3 should be the first gap


(comment

  (next-reqid scanner-subscriptions)
  (next-reqid [])
  (next-reqid nil))
