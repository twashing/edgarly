(ns com.interrupt.component.repl-server
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.nrepl.server :refer [start-server stop-server] :as nrepl]
            [cider.nrepl :refer [cider-middleware]]
            [refactor-nrepl.middleware :refer [wrap-refactor]]))

(defrecord ReplServer [port bind]
  component/Lifecycle
  (start [component]
    (assoc component
           :server (start-server :port port
                                 :bind bind
                                 :handler (apply
                                           nrepl/default-handler
                                           #_#'pb/wrap-cljs-repl
                                           (conj (map resolve cider-middleware)
                                                 wrap-refactor)))))
  (stop [{server :server :as component}]
    (when server
      (stop-server server)
      component)))

(defn new-repl-server
  ([port]
   (new-repl-server port "localhost"))
  ([port bind]
   (map->ReplServer {:port port :bind bind}) ))
