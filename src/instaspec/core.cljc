(ns instaspec.core
  #?(:cljs (:require-macros [instaspec.core]))
  (:require
    [clojure.pprint :as pprint]))


(defmacro match [grammar x r]
  `((:malli-parser ~grammar) ~x))

(defmacro defn [fn-name args & body]
  '...
  )
