(ns instaspec.basic-parsing-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [instaspec.malli :as ism]
            [instaspec.test.helper :as h]))

(use-fixtures :each h/monotone)

(def parse-string (ism/parser '{start <string>}))
(def validate-string (ism/validator '{start <string>}))

(deftest predicate-test
  (h/is-parsed '{start <string>}
               "this is a string"
               "this is a string")
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo
                  :cljs js/Error)
               (h/is-parsed '{start <string>} 1 :fail)))
  (binding [ism/*parse-fail* (fn [schema data] :fail)]
    (is (= :fail
           (parse-string 1))))
  (is (false? (validate-string 1)))
  (is (true? (validate-string "this is a string"))))

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
               '[k :k]))

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

(deftest subvector-rule-test
  (h/is-parsed '{start [a [b c vv]]
                 vv    [d]}
               '[1 [2 3 [4]]]
               '{a 1, b 2, c 3, vv {d 4}}))

(deftest subgroup-rule-test
  (h/is-parsed '{start [a b]
                 b     (c d)}
               '[1 2 3]
               '{a 1, b {c 2, d 3}}))

(ism/schema
  '{node    (or literal tree)
    tree    [tag node*]
    literal (or <int> <string>)})

(ism/schema
  '{node     (or literal tree)
    tree     [tag children]
    children node*
    literal  (or <int> <string>)})

(ism/schema
  '{node     (or literal tree)
    tree     [tag children*]
    children node
    literal  (or <int> <string>)})

(ism/schema
  '{node    literal|tree
    tree    [tag children<node>*]
    literal <int>|<string>})

(deftest recursive-rule-test
  (h/is-parsed '{node    (or literal tree)
                 tree    [tag node*]
                 literal (or <int> <string>)}
               '[:t 2 3 [:st 4]]
               '[tree {tag   :t
                       node* [[literal 2]
                              [literal 3]
                              [tree {tag   :st
                                     node* [[literal 4]]}]]}]))

#_(ma/parse
    '[:schema {:registry {"start" [:and vector? [:catn
                                                 [a [:schema any?]]
                                                 [b [:schema [:ref "b"]]]]],
                          "b"     [:catn [c [:schema any?]] [d [:schema any?]]]}}
      "start"],
    '[1 (2 3)])

#_(ma/parse
    '[:schema {:registry {"start" [:and vector? [:catn
                                                 [a [:schema any?]]
                                                 [b [:catn [c [:schema any?]] [d [:schema any?]]]]]]}}
      "start"],
    '[1 2 3])

(deftest generate-test
  (is (string? (ism/generate '{start <string>}))))
