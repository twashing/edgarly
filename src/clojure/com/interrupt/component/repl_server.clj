(ns com.interrupt.component.repl-server
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.nrepl.server :refer [start-server stop-server] :as nrepl]
            [cider.nrepl :refer [cider-middleware]]))

(defrecord ReplServer [port bind]
  component/Lifecycle
  (start [component]
    (assoc component
           :server (start-server :port port
                                 :bind bind
                                 :handler (apply
                                           nrepl/default-handler
                                           #_#'pb/wrap-cljs-repl
                                           (map resolve cider-middleware)))))
  (stop [{server :server :as component}]
    (when server
      (stop-server server)
      component)))

(defn new-repl-server
  ([port]
   (new-repl-server port "localhost"))
  ([port bind]
   (map->ReplServer {:port port :bind bind}) ))
