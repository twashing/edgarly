(defproject com.interrupt/ibgateway "0.1.0-SNAPSHOT"
  :description "Platform code for the edgar trading system"
  :url "https://github.com/twashing/ibgateway"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repositories [#_["my.datomic.com" {:url "https://my.datomic.com/repo"
                                      :creds :gpg}]
                 ["myMavenRepo.read" "https://mymavenrepo.com/repo/HaEY4usKuLXXnqmXBr0z"]
                 ["myMavenRepo.write" "https://mymavenrepo.com/repo/xc9d5m3WdTIFAqIiiYkn/"]]

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; explicit versions of deps that would cause transitive dep conflicts
                 [org.clojure/tools.reader "1.0.0-beta1"]
                 [slingshot "0.12.2"]
                 [clj-time "0.9.0"]
                 ;; end explicit versions of deps that would cause transitive dep conflicts

                 #_[org.clojure/tools.logging "0.3.1"]
                 [aero "1.1.2"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.danielsz/system "0.4.1-SNAPSHOT"]

                 [com.interactivebrokers.tws/tws-api "9.72.17-SNAPSHOT"]
                 [compojure "1.5.0"]
                 [org.clojure/core.async "0.3.441"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/math.combinatorics "0.1.4"]

                 [org.apache.kafka/kafka_2.11 "0.10.1.1" :exclusions [org.slf4j/slf4j-log4j12]]
                 [org.onyxplatform/onyx "0.10.0-beta12"]
                 [org.onyxplatform/onyx-kafka "0.10.0.0-beta12"]
                 [ymilky/franzy "0.0.1"]
                 [ymilky/franzy-transit "0.0.1"]
                 [ymilky/franzy-admin "0.0.1" :exclusions [org.slf4j/slf4j-api]]

                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.rpl/specter "1.0.0"]
                 #_[com.datomic/clj-client "0.8.606"]
                 #_[com.datomic/datomic-pro "0.9.5561"]
                 [com.datomic/datomic-free "0.9.5561"]

                 #_[com.interrupt/edgarly "0.1.2-SNAPSHOT"]]

  :source-paths ["src/clojure" "test/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[clj-http "3.0.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [suspendable "0.1.1"]
                                  [ring-mock "0.1.5"]
                                  [spyscope "0.1.5"]
                                  #_[embedded-kafka "0.3.3"]

                                  [org.clojure/test.check "0.9.0"]
                                  [clojure-future-spec "1.9.0-alpha17"]
                                  [plumula/mimolette "0.2.1"]
                                  [spec-provider "0.4.9"]]
                   :resource-paths ["resources"]}}

  :repl-options {:init-ns user}
  :injections [(require 'spyscope.core)]

  :main com.interrupt.ibgateway.core)
