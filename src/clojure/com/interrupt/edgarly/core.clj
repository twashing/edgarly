(ns com.interrupt.edgarly.core
  (:require  [com.stuartsierra.component :as component]
             [system.repl :refer [set-init! init start stop reset refresh system]]
             [system.components.repl-server :refer [new-repl-server]]
             [com.interrupt.component.ewrapper :refer [new-ewrapper]]

             [clojure.pprint :refer [pprint]]))

(defn system-map []
  (component/system-map
   :nrepl (new-repl-server 7888 "0.0.0.0")  ;; useful when operating to the cloud
   ;; :ewrapper (new-ewrapper)
   ))

(set-init! #'system-map)


(defn -main [& args]
  (start))
