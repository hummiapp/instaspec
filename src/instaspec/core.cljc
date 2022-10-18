(ns instaspec.core
  #?(:cljs (:require-macros [instaspec.core]))
  (:require
    [clojure.pprint :as pprint]))


(defmacro match [grammar x r]
  `((:malli-parser ~grammar) ~x))

(defmacro defn [fn-name args & body]
  '...
  )

'{start   (or literal tree)
  tree    [tag attrs? start*]
  tag     keyword?
  attrs   {}
  literal (or nil? boolean? number? string?)}

;; TODO: remove by providing a nil transform function?
