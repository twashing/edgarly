(def ks-version "1.3.0")
(def tk-version "1.3.1")
(def tk-jetty9-version "1.5.5")

(defproject com.interrupt/edgarly "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;; :pedantic? :abort

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; explicit versions of deps that would cause transitive dep conflicts
                 [org.clojure/tools.reader "1.0.0-beta1"]
                 [slingshot "0.12.2"]
                 [clj-time "0.9.0"]
                 ;; end explicit versions of deps that would cause transitive dep conflicts


                 [compojure "1.5.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [puppetlabs/trapperkeeper ~tk-version]
                 [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty9-version]

                 [com.interactivebrokers.tws/tws-api "9.72.05"]]

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[puppetlabs/trapperkeeper ~tk-version :classifier "test" :scope "test"]
                                  [puppetlabs/kitchensink ~ks-version :classifier "test" :scope "test"]
                                  [clj-http "3.0.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [ring-mock "0.1.5"]]}}

  :repl-options {:init-ns user}

  :aliases {"tk" ["trampoline" "run" "--config" "dev-resources/config.conf"]}

  :main puppetlabs.trapperkeeper.main)
