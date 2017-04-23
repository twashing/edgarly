(ns com.interrupt.streaming.one
  (:require [clojure.java.io :as io]
            [aero.core :as aero]
            #_[franzy.admin.zookeeper.client :as client]
            #_[franzy.admin.topics :as topics]
            #_[franzy.serialization.serializers :as serializers]
            #_[franzy.serialization.deserializers :as deserializers]

            #_[franzy.clients.producer.client :as producer]
            #_[franzy.clients.consumer.client :as consumer]

            #_[franzy.clients.producer.defaults :as pd]
            #_[franzy.clients.consumer.defaults :as cd]

            #_[franzy.clients.producer.protocols :refer :all]
            #_[franzy.clients.consumer.protocols :refer :all]
            #_[kafka.streams.lambdas :refer :all]

            #_[clojure.core.async :refer (<!!)] 
            #_[datomic.client :as dc]

            [clojure.pprint :refer [pprint]])

  #_(:import [java.util Map HashMap Properties]
           [org.apache.kafka.streams StreamsConfig KafkaStreams]
           [org.apache.kafka.streams.kstream KStreamBuilder KStream KTable]
           [org.apache.kafka.common.serialization Serde Serdes]))

(comment

  #_(def conn
    (<!! (dc/connect
          {:db-name "hello"

           ;; :account-id client/PRO_ACCOUNT
           ;; :secret "mysecret"
           ;; :access-key "myaccesskey"
           ;; :region "none"

           :endpoint "datomic:free://edgarly_datomic_1:4334"
           :service "peer-server"})))


  (require '[datomic.api :as d])
  (def db-uri "datomic:mem://goodbye")
  (def result (d/create-database db-uri))
  (def conn (d/connect db-uri))

  (require '[datomic.api :as d])
  (def db-uri "datomic:free://edgarly_datomic_1:4334/goodbye")
  (def result (d/create-database db-uri))
  (def conn (d/connect db-uri))

  (require '[datomic.api :as d])
  (def db-uri "datomic:free://localhost:4334/hello")
  (def result (d/create-database db-uri))
  (def conn (d/connect db-uri))


  {:com.interrupt.edgarly.core/reqid 1
   :com.interrupt.edgarly.core/scan-name "HIGH_OPT_IMP_VOLAT"
   :com.interrupt.edgarly.core/scan-symbol "YTEN"
   :com.interrupt.edgarly.core/scan-rank 0
   :com.interrupt.edgarly.core/tag :volatility}

  {:db/ident :com.interrupt.edgarly.core/reqid
   :db/valueType :db.type/long
   :db/cardinality :db.cardinality/one
   :db/doc "The TWS Gateway request id of the data being pulled"}

  ;; **
  {:db/ident :com.interrupt.edgarly.core/scan-name
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one
   :db/doc "The name of the market scan request, made to IB"}

  {:db/ident :com.interrupt.edgarly.core/scan-symbol
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/many
   :db/doc "The scan result, contains stock symbols, and thier rank, according to the scan type."}

  {:db/ident :com.interrupt.edgarly.core/scan-rank
   :db/valueType :db.type/long
   :db/cardinality :db.cardinality/many
   :db/doc "The scan result, contains stock symbols, and thier rank, according to the scan type."}

  {:db/ident :com.interrupt.edgarly.core/tag
   :db/valueType :db.type/keyword
   :db/cardinality :db.cardinality/one
   :db/doc "The type of scan being made"}

  )

(comment

  (def streamsConfiguration (Properties.))
  (.put streamsConfiguration StreamsConfig/APPLICATION_ID_CONFIG "wordcount-lambda-example")
  (.put streamsConfiguration StreamsConfig/BOOTSTRAP_SERVERS_CONFIG "edgarly_kafka_1:9092")
  (.put streamsConfiguration StreamsConfig/ZOOKEEPER_CONNECT_CONFIG "edgarly_zookeeper_1:2181")
  (.put streamsConfiguration StreamsConfig/KEY_SERDE_CLASS_CONFIG (-> (Serdes/String) .getClass .getName))
  (.put streamsConfiguration StreamsConfig/VALUE_SERDE_CLASS_CONFIG (-> (Serdes/String) .getClass .getName))

  (def stringSerde (Serdes/String))
  (def longSerde (Serdes/Long))

  (def builder (KStreamBuilder.))
  (def ^KStream textLines (.stream
                           ^KStreamBuilder builder
                           ^Serde stringSerde
                           ^Serde stringSerde
                           ^"[Ljava.lang.String;" (into-array String ["one"])))

  (def ^KStream wordCounts (-> textLines
                             (.flatMapValues (value-mapper identity))
                             #_(.toStream)))

  (.to wordCounts stringSerde longSerde "two")

  (def streams (KafkaStreams. builder streamsConfiguration))
  (.start streams))

(defn one []

  ;; === Topics
  (def zk-utils (client/make-zk-utils {:servers "edgarly_zookeeper_1:2181"} false))
  (def two (topics/create-topic! zk-utils "scanner-start" 10))
  (def three (topics/create-topic! zk-utils "scanner-stop" 10))

  (def four (topics/create-topic! zk-utils "scanner-start-2" 10))
  (def five (topics/create-topic! zk-utils "scanner-out" 10))
  (def one (topics/all-topics zk-utils)))

(defn start-app []

  ;; === Topology
  (def builder (KStreamBuilder.))
  #_(def ^GlobalKTable scanner-start (.globalTable builder (Serdes/Long) (Serdes/String) "scanner-start" "scanner-start"))

  (def ^KTable scanner-start-2 (.table builder "scanner-start-2" "scanner-start-2"))
  (def ^KTable scanner-stop (.table builder "scanner-stop" "scanner-stop"))

  (def one (.join scanner-stop
                  scanner-start-2
                  (value-joiner
                   (fn [a b]
                     (println "Join / a: " a " / b: " b)
                     [a b]
                     #_(if (= a b)
                       a
                       #_[(:id a)
                          {(:id a) :match
                           :request-id 1} ])))))

  (def key-serializer (serializers/keyword-serializer))
  (def value-serializer (serializers/edn-serializer))

  (def stringSerde (Serdes/String))
  (def longSerde (Serdes/Long))
  #_(.to wordCounts stringSerde longSerde "two")

  (.to one stringSerializer stringSerializer "scanner-out")

  ;; i. subscribe to IB, ii. track "Request IDs"
  (def streamsConfiguration (Properties.))
  (.put streamsConfiguration StreamsConfig/APPLICATION_ID_CONFIG "edgarly")
  (.put streamsConfiguration StreamsConfig/BOOTSTRAP_SERVERS_CONFIG "edgarly_kafka_1:9092")
  (.put streamsConfiguration StreamsConfig/ZOOKEEPER_CONNECT_CONFIG "edgarly_zookeeper_1:2181")

  (def streams (KafkaStreams. builder streamsConfiguration))
  (.cleanUp streams)
  (.start streams))

(defn two []

  (let [;; Use a vector if you wish for multiple servers in your cluster
        pc {:bootstrap.servers ["edgarly_kafka_1:9092"]}

        ;;Serializes producer record keys that may be keywords
        key-serializer (serializers/keyword-serializer)

        ;;Serializes producer record values as EDN, built-in
        value-serializer (serializers/edn-serializer)

        ;;optionally create some options, even just use the defaults explicitly
        ;;for those that don't need anything fancy...
        options (pd/make-default-producer-options)
        topic "scanner-start-2"
        partition 0]

    (with-open [p (producer/make-producer pc key-serializer value-serializer options)]
      (let [send-fut (send-async! p topic partition :a {:foo :bar} #_:inconceivable #_{:things-in-fashion
                                                                                       [:masks :giants :kerry-calling-saul]} options)

            #_record-metadata #_(send-sync! p "land-wars-in-asia" 0 :conceivable
                                            {:deadly-poisons [:iocaine-powder :ska-music :vegan-cheese]}
                                            options)

            ;;we can also use records to produce, wrapping our per producer record value (data) as usual
            #_record-metadata-records #_(send-sync! p (pt/->ProducerRecord topic partition :vizzini
                                                                           {:quotes ["the battle of wits has begun!"
                                                                                     "finish him, your way!" ]})
                                                    options)]

        #_(println "Sync send results:" record-metadata)
        #_(println "Sync send results w/record:" record-metadata-records)
        (println "Async send results:" @send-fut))))  

  (let [;; Use a vector if you wish for multiple servers in your cluster
        pc {:bootstrap.servers ["edgarly_kafka_1:9092"]}

        ;;Serializes producer record keys that may be keywords
        key-serializer (serializers/keyword-serializer)

        ;;Serializes producer record values as EDN, built-in
        value-serializer (serializers/edn-serializer)

        ;;optionally create some options, even just use the defaults explicitly
        ;;for those that don't need anything fancy...
        options (pd/make-default-producer-options)
        topic "scanner-stop"
        partition 0]

    (with-open [p (producer/make-producer pc key-serializer value-serializer options)]
      (let [send-fut (send-async! p topic partition :a {:qwerty :zxcv} #_:inconceivable #_{:things-in-fashion
                                                                                           [:masks :giants :kerry-calling-saul]} options)

            #_record-metadata #_(send-sync! p "land-wars-in-asia" 0 :conceivable
                                            {:deadly-poisons [:iocaine-powder :ska-music :vegan-cheese]}
                                            options)

            ;;we can also use records to produce, wrapping our per producer record value (data) as usual
            #_record-metadata-records #_(send-sync! p (pt/->ProducerRecord topic partition :vizzini
                                                                           {:quotes ["the battle of wits has begun!"
                                                                                     "finish him, your way!" ]})
                                                    options)]

        #_(println "Sync send results:" record-metadata)
        #_(println "Sync send results w/record:" record-metadata-records)
        (println "Async send results:" @send-fut)))))

(defn three []

  (let [cc {:bootstrap.servers       ["edgarly_kafka_1:9092" ]
            :group.id                "submissive-blonde-aussies"
            ;;jump as early as we can, note this isn't necessarily 0
            :auto.offset.reset       :earliest
            ;;here we turn on committing offsets to Kafka itself, every 1000 ms
            :enable.auto.commit      true
            :auto.commit.interval.ms 1000}

        key-deserializer (deserializers/keyword-deserializer)
        value-deserializer (deserializers/edn-deserializer)
        topic "scanner-out"

        ;;Here we are demonstrating the use of a consumer rebalance listener. Normally you'd use this with a manual consumer to deal with offset management.
        ;;As more consumers join the consumer group, this callback should get fired among other reasons.
        ;;To implement a manual consumer without this function is folly, unless you care about losing data, and probably your job.
        ;;One could argue though that most data is not as valuable as we are told. I heard this in a dream once or in intro to Philosophy.
        ;; rebalance-listener (consumer-rebalance-listener (fn [topic-partitions]
        ;;                                                   (println "topic partitions assigned:" topic-partitions))
        ;;                                                 (fn [topic-partitions]
        ;;                                                   (println "topic partitions revoked:" topic-partitions)))
        ;;We create custom producer options and set out listener callback like so.
        ;;Now we can avoid passing this callback every call that requires it, if we so desire
        ;;Avoiding the extra cost of creating and garbage collecting a listener is a best practice
        options (cd/make-default-consumer-options {} #_{:rebalance-listener-callback rebalance-listener})]

    (with-open [c (consumer/make-consumer cc key-deserializer value-deserializer options)]
      ;;Note! - The subscription will read your comitted offsets to position the consumer accordingly
      ;;If you see no data, try changing the consumer group temporarily
      ;;If still no, have a look inside Kafka itself, perhaps with franzy-admin!
      ;;Alternatively, you can setup another threat that will produce to your topic while you consume, and all should be well
      (subscribe-to-partitions! c [topic])
      ;;Let's see what we subscribed to, we don't need Cumberbatch to investigate here...
      (println "Partitions subscribed to:" (partition-subscriptions c))
      ;;now we poll and see if there's any fun stuff for us
      (let [cr (poll! c)
            ;;a naive transducer, written the long way
            filter-xf (filter (fn [cr] (= (:key cr) :inconceivable)))
            ;;a naive transducer for viewing the values, again long way
            value-xf (map (fn [cr] (:value cr)))
            ;;more misguided transducers
            inconceivable-transduction (comp filter-xf value-xf)]

        (println "Record count:" (record-count cr))
        (println "Records by topic:" (records-by-topic cr topic))
      ;;;The source data is a seq, be careful!
        (println "Records from a topic that doesn't exist:" (records-by-topic cr "no-one-of-consequence"))
        (println "Records by topic partition:" (records-by-topic-partition cr topic 0))
      ;;;The source data is a list, so no worries here....
        (println "Records by a topic partition that doesn't exist:" (records-by-topic-partition cr "no-one-of-consequence" 99))
        (println "Topic Partitions in the result set:" (record-partitions cr))
        (clojure.pprint/pprint (into [] inconceivable-transduction cr))
                                        ;(println "Now just the values of all distinct records:")
        (println "Put all the records into a vector (calls IReduceInit):" (into [] cr))
        ;;wow, that was tiring, maybe now we don't want to listen anymore to this topic and take a break, maybe subscribe
        ;;to something else next poll....
        (clear-subscriptions! c)
        (println "After clearing subscriptions, a stunning development! We are now subscribed to the following partitions:"
                 (partition-subscriptions c)))))
  )

(comment

  ;; TOPICS
  (one)

  (start-app)

  ;; PRODUCE Messages
  (two)

  ;; CONSUME Message
  (three)

  ;; produce
  ;; consume
  ;; transit serde


  )
