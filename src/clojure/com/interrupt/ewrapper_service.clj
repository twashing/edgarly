(ns com.interrupt.ewrapper-service
  (:require [clojure.tools.logging :as log]
            [puppetlabs.trapperkeeper.core :refer [defservice]]
            [com.interrupt.ewrapper :refer [ewrapper]]))


(defservice ewrapper-service
  []
  (init [this context]
        (println "ewrapper-service initializing")
        context)

  (start [this context]
         (log/infof "ewrapper-service started; See: %s:%s" 1 2)
         (assoc context :ewrapper (ewrapper))))

