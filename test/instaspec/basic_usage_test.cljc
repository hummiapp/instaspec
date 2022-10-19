(ns instaspec.basic-usage-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [instaspec.test.helper :as h]))

(use-fixtures :each h/monotone)

(deftest predicate-test
  (h/is-parsed '{start <string>}
               "this is a string"
               "this is a string")
  (h/is-parsed '{start <string>}
               1
               nil))

(deftest collection-parsing-test
  (h/is-parsed '{start [a b c]}
               [1 2 3]
               '{a 1, b 2, c 3})
  (h/is-parsed '{start (a b c)}
               '(1 2 3)
               '{a 1, b 2, c 3})
  (h/is-parsed '{start {}}
               '{:a 1, :b 2, :c 3}
               '{:a 1, :b 2, :c 3})
  (h/is-parsed '{start #{}}
               '#{:a :b :c}
               '#{:a :b :c}))

(deftest sub-rules-test
  (h/is-parsed '{start [m s v]
                 m     {}
                 s     #{}
                 v     [a b c]}
               '[{:a 1} #{:a} [1 2 3]]
               '{m {:a 1}
                 s #{:a}
                 v {a 1, b 2, c 3}}))

(deftest or-rules-test
  (h/is-parsed '{start (or v k)
                 v     [a b c]
                 k     <keyword>}
               ':k
               ':k))

(deftest sub-or-rules-test
  (h/is-parsed '{start [m s (or v k)]
                 m     {}
                 s     #{}
                 v     [a b c]
                 k     <keyword>}
               '[{:a 1} #{:a} :k]
               '{m {:a 1}
                 s #{:a}
                 k :k}))

(deftest subseq-test
  (h/is-parsed '{start [a [b c [d]]]}
               '[1 [2 3 [4]]]
               '{a 1, b 2, c 3, d 4}))

;; orn it up?
