(ns com.interrupt.edgarly-web-service
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [com.interrupt.edgarly-web-core :as core]
            [puppetlabs.trapperkeeper.core :refer [defservice]]
            [puppetlabs.trapperkeeper.services :as tk-services]))

(defservice hello-web-service
  [[:ConfigService get-in-config]
   [:WebroutingService add-ring-handler get-route]
   HelloService]
  (init [this context]
    (log/info "Initializing hello webservice")
    (let [url-prefix (get-route this)]
      (add-ring-handler
        this
        (compojure/context url-prefix []
          (core/app (tk-services/get-service this :HelloService))))
      (assoc context :url-prefix url-prefix)))

  (start [this context]
         (let [host (get-in-config [:webserver :host])
               port (get-in-config [:webserver :port])
               url-prefix (get-route this)]
              (log/infof "Hello web service started; visit http://%s:%s%s/world to check it out!"
                         host port url-prefix))
         context))
