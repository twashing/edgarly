(ns com.interrupt.test
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [clojure.spec.test]))

(defmacro defspec-test
  ([name sym-or-syms] `(defspec-test ~name ~sym-or-syms nil))
  ([name sym-or-syms opts]
   (when t/*load-tests*
     `(def ~(vary-meta name assoc
                       :test `(fn []
                                (let [check-results# (clojure.spec.test/check ~sym-or-syms ~opts)
                                      checks-passed?# (every? nil? (map :failure check-results#))]
                                  (if checks-passed?#
                                    (t/do-report {:type    :pass
                                                  :message (str "Generative tests pass for "
                                                                (str/join ", " (map :sym check-results#)))})
                                    (doseq [failed-check# (filter :failure check-results#)
                                            :let [r# (clojure.spec.test/abbrev-result failed-check#)
                                                  failure# (:failure r#)]]
                                      (t/do-report
                                        {:type     :fail
                                         :message  (with-out-str (clojure.spec/explain-out failure#))
                                         :expected (->> r# :spec rest (apply hash-map) :ret)
                                         :actual   (if (instance? Throwable failure#)
                                                     failure#
                                                     (:clojure.spec.test/val failure#))})))
                                  checks-passed?#)))
        (fn [] (t/test-var (var ~name)))))))


