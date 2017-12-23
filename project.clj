(defproject com.interrupt/edgarly "0.1.0-SNAPSHOT"
  :description "Platform code for the edgar trading system"
  :url "https://github.com/twashing/edgarly"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repositories [["myMavenRepo.read" "https://mymavenrepo.com/repo/HaEY4usKuLXXnqmXBr0z"]
                 ["myMavenRepo.write" "https://mymavenrepo.com/repo/xc9d5m3WdTIFAqIiiYkn/"]]

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; explicit versions of deps that would cause transitive dep conflicts
                 [org.clojure/tools.reader "1.0.0-beta1"]
                 [slingshot "0.12.2"]
                 [clj-time "0.9.0"]
                 ;; end explicit versions of deps that would cause transitive dep conflicts

                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/data.json "0.2.6"]
                 [com.stuartsierra/component "0.3.2"]
                 [aero "1.1.2"]
                 [compojure "1.5.0" :exclusions [commons-codec]]
                 [org.danielsz/system "0.4.1-SNAPSHOT"
                  :exclusions [org.clojure/tools.reader org.clojure/core.async org.clojure/tools.namespace]]

                 [com.interactivebrokers.tws/tws-api "9.72.17-SNAPSHOT"]
                 [org.clojure/core.async "0.3.441" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/math.combinatorics "0.1.4"]

                 [org.onyxplatform/onyx "0.12.0" :exclusions [org.clojure/tools.reader org.clojure/core.async joda-time
                                                                    prismatic/schema commons-codec]]
                 [org.onyxplatform/onyx-kafka "0.12.0.0" :exclusions [log4j org.clojure/tools.reader org.clojure/core.async
                                                                            joda-time org.scala-lang/scala-library commons-codec]]
                 [ymilky/franzy "0.0.1"]
                 [ymilky/franzy-transit "0.0.1" :exclusions [commons-codec]]
                 [ymilky/franzy-admin "0.0.1" :exclusions [log4j]]

                 [com.rpl/specter "1.0.0"]
                 [com.datomic/datomic-free "0.9.5561" :exclusions [org.slf4j/log4j-over-slf4j]]]

  :source-paths ["src/clojure" "test/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[clj-http "3.0.0" :exclusions [commons-codec]]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [suspendable "0.1.1"]
                                  [ring-mock "0.1.5" :exclusions [commons-codec]]
                                  [spyscope "0.1.5"]
                                  [org.clojure/test.check "0.9.0"]
                                  [clojure-future-spec "1.9.0-alpha17"]
                                  [plumula/mimolette "0.2.1"]
                                  [spec-provider "0.4.9" :exclusions [org.clojure/clojure]]]
                   :resource-paths ["resources"]}}

  :repl-options {:init-ns user}
  :main com.interrupt.edgarly.core)
