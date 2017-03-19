 (defproject com.interrupt/edgarly "0.1.0-SNAPSHOT"
  :description "Platform code for the edgar trading system"
   :url "https://github.com/twashing/edgarly"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;; :pedantic? :abort
  :repositories [["myMavenRepo.read" "https://mymavenrepo.com/repo/HaEY4usKuLXXnqmXBr0z"]]

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; explicit versions of deps that would cause transitive dep conflicts
                 [org.clojure/tools.reader "1.0.0-beta1"]
                 [slingshot "0.12.2"]
                 [clj-time "0.9.0"]
                 ;; end explicit versions of deps that would cause transitive dep conflicts

                 [org.clojure/tools.logging "0.3.1"]

                 [com.stuartsierra/component "0.3.2"]
                 [org.danielsz/system "0.4.1-SNAPSHOT"]

                 [com.interactivebrokers.tws/tws-api "9.72.17-SNAPSHOT"]
                 [compojure "1.5.0"]
                 [org.clojure/core.async "0.3.441"]
                 [org.clojure/core.match "0.3.0-alpha4"]]

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[clj-http "3.0.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [suspendable "0.1.1"]
                                  [ring-mock "0.1.5"]]}}

  :repl-options {:init-ns user}

   :main com.interrupt.edgarly.core)
