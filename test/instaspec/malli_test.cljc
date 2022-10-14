(ns instaspec.malli-test
  (:require [clojure.test :refer [deftest is testing]]
            [instaspec.malli :as im]))

(deftest rule-test
  (is (= '[:or nil?] (im/rule '(or nil?))))
  (is (= '[:catn
           [a [:schema [:ref "a"]]]
           [b [:schema [:ref "b"]]]
           [c [:schema [:ref "c"]]]]
         (im/rule '[a b c]))))
