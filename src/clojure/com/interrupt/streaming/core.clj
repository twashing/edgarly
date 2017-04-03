(ns com.interrupt.streaming.core
  (:require [clojure.java.io :as io]
            [aero.core :as aero]
            [com.interrupt.streaming.topology :refer [start-topologies]]))


(comment

  (require '[kafka.streams.lambdas :refer :all])
  (import '[java.util Map HashMap Properties]
          '[org.apache.kafka.streams StreamsConfig KafkaStreams]
          '[org.apache.kafka.streams.kstream KStreamBuilder KStream]
          '[org.apache.kafka.common.serialization Serde Serdes])

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
