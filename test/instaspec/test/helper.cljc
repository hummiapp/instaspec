(ns instaspec.test.helper
  #?(:cljs (:require-macros [instaspec.test.helper]))
  (:require [clojure.pprint :as pprint]
            #?(:clj [clojure.test :refer [is]])))

(def ^:dynamic *debug* false)

(defn monotone [test]
  (let [n (atom 0)]
    (with-redefs [gensym (fn [prefix]
                           (symbol (str prefix (swap! n inc))))]
      (test))))

(defn report [label x]
  (when *debug*
    (println)
    (println "*** " label " ***")
    (pprint/pprint x)))

#?(:clj
   (defmacro is-parsed [grammar data expected]
     `(let [s# (ism/schema ~grammar)
            _# (report "Schema" s#)
            p# (ism/parser ~grammar)
            r# (p# ~data)]
        (report "Result" r#)
        (is (= ~expected r#)))))
