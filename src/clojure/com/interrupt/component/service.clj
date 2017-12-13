(ns com.interrupt.component.service
  (:require [com.stuartsierra.component :as component]

            [franzy.clients.producer.client :as producer]
            [franzy.clients.consumer.client :as consumer]
            [franzy.clients.producer.defaults :as pd :refer [make-default-producer-options]]
            [franzy.clients.consumer.defaults :as cd]
            [franzy.clients.consumer.callbacks :refer [consumer-rebalance-listener]]
            [franzy.clients.consumer.results :as consumer-results]

            [franzy.clients.producer.protocols :refer :all]
            [franzy.clients.consumer.protocols :refer :all]
            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]

            [com.interrupt.streaming.platform :as pl]
            [com.interrupt.streaming.platform.scanner-command :as psc]
            [com.interrupt.streaming.platform.scanner :as ps]
            [aero.core :refer [read-config]]))


(defrecord Service []
  component/Lifecycle

  (start [component]

    (println ";; Starting Service")
    (assoc component :service :foo))

  (stop [component]

    (println ";; Stopping Service")
    (dissoc component :service)))

(defn new-service []
  (map->Service {}))

(defn write-to-topic
  ([topic] (write-to-topic topic "55ff56cc-77d9-4b11-af01-fb79e2ee3a10" {:foo :bar}))
  ([topic k v]
   (let [;; Use a vector if you wish for multiple servers in your cluster
         pc {:bootstrap.servers ["kafka:9092"]
             :group.id          "group.one"}

         ;; Serializes producer record keys that may be keywords
         string-serializer (serializers/string-serializer)

         ;;Serializes producer record values as EDN, built-in
         value-serializer (serializers/edn-serializer)

         ;;optionally create some options, even just use the defaults explicitly
         ;;for those that don't need anything fancy...
         options (pd/make-default-producer-options)
         partition 0]

     (with-open [p (producer/make-producer pc string-serializer value-serializer options)]
       (let [send-fut (send-async! p topic partition k v options)]
         (println "Async send results:" @send-fut))))))

(defn read-from-topic-manual [topic]

  (let [cc {:bootstrap.servers ["kafka:9092"]
            :group.id          "group.one"
            :auto.offset.reset :earliest}

        ;; notice now we are using keywords, to ensure things go as we planned when serializing
        key-deserializer (deserializers/keyword-deserializer)

        ;; notice now we are using an EDN deserializer to ensure we get back the data correctly
        value-deserializer (deserializers/edn-deserializer)
        options (cd/make-default-consumer-options)

        ;; notice, we are setting the topics and partitions to what we produced to earlier...
        topic-partitions [{:topic topic :partition 0}]]

    (with-open [c (consumer/make-consumer cc key-deserializer value-deserializer options)]

      ;; first, lets get some information about the currently available topic partitions...
      ;; we will see a list of topics, along with partition info for each one
      #_(println "Topic Partition Info per Topic:" (list-topics c))

      ;;maybe you just want an eager list of topics, that's it....a simple solution with many possible solutions
      #_(println "An inefficient vector of topics:" (->> (list-topics c)
                                                       ;;or (keys), but here we want to stringify our keys a bit
                                                       (into [] (map (fn [[k _]] (name k))))))

      ;; something more specific in scope
      #_(println "Topic Partitions for our topic:" (partitions-for c topic))

      ;; now let us manually assign a partition
      ;; if you really wanted some dynamic behavior, you could use some of the results above from list-topics
      (assign-partitions! c topic-partitions)

      ;; list the assigned partitions - shocking revelations follow:
      (println "Assigned Partitions:" (assigned-partitions c))

      ;; now lets say we don't like to be labeled, and thus, we don't want any more assignments
      (println "Clearing partition assignments....")
      (clear-partition-assignments! c)
      (println "After clearing all partition assignments, we have exactly this many assignments (correlates to wall-street accountability):"
               (assigned-partitions c)))))

(defn read-from-topic-subscription [topic]

  (let [cc {:bootstrap.servers       ["kafka:9092"]
            :group.id                "group.one"

            ;; jump as early as we can, note this isn't necessarily 0
            :auto.offset.reset       :earliest

            ;; here we turn on committing offsets to Kafka itself, every 1000 ms
            :enable.auto.commit      true
            :auto.commit.interval.ms 1000}
        key-deserializer (deserializers/keyword-deserializer)
        value-deserializer (deserializers/edn-deserializer)

        ;; Here we are demonstrating the use of a consumer rebalance listener. Normally you'd use this with a manual consumer to deal with offset management.
        ;; As more consumers join the consumer group, this callback should get fired among other reasons.
        ;; To implement a manual consumer without this function is folly, unless you care about losing data, and probably your job.
        ;; One could argue though that most data is not as valuable as we are told. I heard this in a dream once or in intro to Philosophy.
        rebalance-listener (consumer-rebalance-listener (fn [topic-partitions]
                                                          (println "topic partitions assigned:" topic-partitions))
                                                        (fn [topic-partitions]
                                                          (println "topic partitions revoked:" topic-partitions)))

        topic-partitions [{:topic topic :partition 0}]

        ;; We create custom producer options and set out listener callback like so.
        ;; Now we can avoid passing this callback every call that requires it, if we so desire
        ;; Avoiding the extra cost of creating and garbage collecting a listener is a best practice
        options (cd/make-default-consumer-options {:rebalance-listener-callback rebalance-listener})]

    (with-open [c (consumer/make-consumer cc key-deserializer value-deserializer options)]

      (assign-partitions! c topic-partitions)

      ;; Note! - The subscription will read your comitted offsets to position the consumer accordingly
      ;; If you see no data, try changing the consumer group temporarily
      ;; If still no, have a look inside Kafka itself, perhaps with franzy-admin!
      ;; Alternatively, you can setup another threat that will produce to your topic while you consume, and all should be well
      #_(subscribe-to-partitions! c [topic])

      ;; Let's see what we subscribed to, we don't need Cumberbatch to investigate here...
      (println "Partitions subscribed to:" (partition-subscriptions c))

      ;; now we poll and see if there's any fun stuff for us
      (let [cr (poll! c)

            ;; a naive transducer, written the long way
            #_filter-xf #_(filter (fn [cr] (= (:key cr) :inconceivable)))

            ;; a naive transducer for viewing the values, again long way
            #_value-xf #_(map (fn [cr] (:value cr)))

            ;; more misguided transducers
            #_inconceivable-transduction #_(comp filter-xf value-xf)]

        (println "Record count:" (record-count cr))
        (println "Records by topic:" (records-by-topic cr topic))

        ;; The source data is a seq, be careful!
        #_(println "Records from a topic that doesn't exist:" (records-by-topic cr "no-one-of-consequence"))
        #_(println "Records by topic partition:" (records-by-topic-partition cr topic 0))

        ;; The source data is a list, so no worries here....
        #_(println "Records by a topic partition that doesn't exist:" (records-by-topic-partition cr "no-one-of-consequence" 99))
        #_(println "Topic Partitions in the result set:" (record-partitions cr))
        #_(clojure.pprint/pprint (into [] inconceivable-transduction cr))

        ;; (println "Now just the values of all distinct records:")
        (println "Put all the records into a vector (calls IReduceInit):" (into [] cr))

        ;; wow, that was tiring, maybe now we don't want to listen anymore to this topic and take a break, maybe subscribe
        ;; to something else next poll....
        #_(clear-subscriptions! c)
        #_(println "After clearing subscriptions, a stunning development! We are now subscribed to the following partitions:"
                 (partition-subscriptions c))))))


(comment

  #_(let [pc {:bootstrap.servers ["kafka:9092"]
            :retries           1
            :batch.size        16384
            :linger.ms         1
            :buffer.memory     33554432}
        ;; normally, just inject these direct and be aware some serializers may need to be closed,
        ;; adding to binding here to make this clear

        ;; Serializes producer record keys as strings
        key-serializer (serializers/string-serializer)

        ;; Serializes producer record values as strings
        value-serializer (serializers/string-serializer)]

    (with-open [p (producer/make-producer pc key-serializer value-serializer)]
      (partitions-for p "scanner-command")))

  #_(let [;; Use a vector if you wish for multiple servers in your cluster
        pc {:bootstrap.servers ["kafka:9092"]}

        ;; Serializes producer record keys that may be keywords
        key-serializer (serializers/keyword-serializer)

        ;; Serializes producer record values as EDN, built-in
        value-serializer (serializers/edn-serializer)

        ;; optionally create some options, even just use the defaults explicitly
        ;; for those that don't need anything fancy...
        options (make-default-producer-options)
        topic "scanner-command"
        partition 0]

    (with-open [p (producer/make-producer pc key-serializer value-serializer options)]
      (let [send-fut (send-async! p topic partition :inconceivable {:things-in-fashion
                                                                    [:masks :giants :kerry-calling-saul]} options)
            #_record-metadata #_(send-sync! p "land-wars-in-asia" 0 :conceivable
                                        {:deadly-poisons [:iocaine-powder :ska-music :vegan-cheese]}
                                        options)

            ;; we can also use records to produce, wrapping our per producer record value (data) as usual
            #_record-metadata-records #_(send-sync! p (pt/->ProducerRecord topic partition :vizzini
                                                                       {:quotes ["the battle of wits has begun!"
                                                                                 "finish him, your way!" ]})
                                                options)]

        (println "Sync send results:" record-metadata)
        (println "Sync send results w/record:" record-metadata-records)
        (println "Async send results:" @send-fut))))

  (write-to-topic "scanner-command" "3341bf41-0377-4d29-a426-6eff33395896" {:foo :bar})

  (read-from-topic-manual "scanner-command")
  (read-from-topic-subscription "scanner-command")



  #_(let [cc {:bootstrap.servers       ["kafka:9092"]
            :group.id                "group.one"
            :auto.offset.reset       :earliest
            :enable.auto.commit      true
            :auto.commit.interval.ms 1000}

        topic "scanner-command"
        key-deserializer (deserializers/keyword-deserializer)
        value-deserializer (deserializers/edn-deserializer)
        rebalance-listener (consumer-rebalance-listener (fn [topic-partitions]
                                                          (println "topic partitions assigned:" topic-partitions))
                                                        (fn [topic-partitions]
                                                          (println "topic partitions revoked:" topic-partitions)))

        options (cd/make-default-consumer-options {:rebalance-listener-callback rebalance-listener})]

    (with-open [c (consumer/make-consumer cc key-deserializer value-deserializer options)]

      (let [cr (some->> (.poll! c 5000)
                        (consumer-results/consumer-records))]

        (println cr)
        (println "Record count:" (record-count cr))
        (println "Records by topic:" (records-by-topic cr topic)))))

  )
