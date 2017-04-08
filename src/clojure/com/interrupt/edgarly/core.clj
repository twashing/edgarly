(ns com.interrupt.edgarly.core
  (:require  [com.stuartsierra.component :as component]
             [system.repl :refer [set-init! init start stop reset refresh system]]
             [system.components.repl-server :refer [new-repl-server]]
             [com.interrupt.component.ewrapper :refer [new-ewrapper]]

             [clojure.pprint :refer [pprint]])
  (:import [java.util.concurrent TimeUnit]))

(defn system-map []
  (component/system-map
   :nrepl (new-repl-server 7888 "0.0.0.0")  ;; useful when operating to the cloud
   ;; :ewrapper (new-ewrapper)
   ))

(set-init! #'system-map)
(defn start-system [] (start))
(defn stop-system [] (stop))

(defn scanner-start [])
(defn scanner-stop [])

(defn historical-start [])
(defn historical-stop [])

(defn market-start [])
(defn market-stop [])

(defn open-request-ids [])

(defn -main [& args]
  (Thread/sleep 10000) ;; a hack, to ensure that the tws machine is available, before we try to connect to it.
  (start-system))
