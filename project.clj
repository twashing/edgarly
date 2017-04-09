(defproject com.interrupt/edgarly "0.1.0-SNAPSHOT"
  :description "Platform code for the edgar trading system"
  :url "https://github.com/twashing/edgarly"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;; :pedantic? :abort
  :repositories [["myMavenRepo.read" "https://mymavenrepo.com/repo/HaEY4usKuLXXnqmXBr0z"]
                 ["confluent" {:url "http://packages.confluent.io/maven/"}]
                 ["snapshots" {:url "https://fundingcircle.artifactoryonline.com/fundingcircle/libs-snapshot-local"
                               :username "tim.washington"
                               :password "APAKktBy8jcwd7YGoJtUn94xsVk"
                               :sign-releases false}]
                 ["releases" {:url "https://fundingcircle.artifactoryonline.com/fundingcircle/libs-release-local"
                              :username "tim.washington"
                              :password "APAKktBy8jcwd7YGoJtUn94xsVk"
                              :sign-releases false}]]

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; explicit versions of deps that would cause transitive dep conflicts
                 [org.clojure/tools.reader "1.0.0-beta1"]
                 [slingshot "0.12.2"]
                 [clj-time "0.9.0"]
                 ;; end explicit versions of deps that would cause transitive dep conflicts

                 [org.clojure/tools.logging "0.3.1"]
                 [aero "1.1.2"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.danielsz/system "0.4.1-SNAPSHOT"]

                 [com.interactivebrokers.tws/tws-api "9.72.17-SNAPSHOT"]
                 [compojure "1.5.0"]
                 [org.clojure/core.async "0.3.441"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/math.combinatorics "0.1.4"]

                 [org.apache.kafka/kafka-streams "0.10.2.0"]
                 #_[fundingcircle/kafka.client "0.4.2"]
                 #_[fundingcircle/kafka.serdes "0.5.3"]
                 [fundingcircle/kafka.streams "0.4.7"]
                 [ymilky/franzy "0.0.1"]
                 [ymilky/franzy-admin "0.0.1" :exclusions [org.slf4j/slf4j-api]]

                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/test.check "0.9.0"]
                 [clojure-future-spec "1.9.0-alpha15"]
                 [spyscope "0.1.5"]]

  :source-paths ["src/clojure" "test/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[clj-http "3.0.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [suspendable "0.1.1"]
                                  [ring-mock "0.1.5"]]
                   :resource-paths ["resources"]}}

  :repl-options {:init-ns user}
  :injections [(require 'spyscope.core)]

  :main com.interrupt.edgarly.core)
