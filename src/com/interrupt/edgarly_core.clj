(ns com.interrupt.edgarly-core)

(defn hello
  "Say hello to caller"
  [caller]
  (format "Hello, %s!" caller))
