(ns com.interrupt.streaming.platform.serialization
  (:require [franzy.serialization.serializers :as serializers]
            [franzy.serialization.deserializers :as deserializers]))


(def deserializer (deserializers/edn-deserializer))
(def serializer (serializers/edn-serializer))
(def string-deserializer (deserializers/string-deserializer))
(def string-serializer (serializers/string-serializer))

(defn deserialize-kafka-message [segments]
  (.deserialize deserializer nil segments))

(defn serialize-kafka-message [segment]
  (.serialize serializer nil segment))

#_(defn deserialize-kafka-key [k] k)
#_(defn serialize-kafka-key [k] k)


(defn deserialize-kafka-key [k]
  (.deserialize string-deserializer nil k))

(defn serialize-kafka-key [k]
  (.serialize string-serializer nil k))
