(ns instaspec.malli-test
  (:require [clojure.test :refer [deftest is testing]]
            [instaspec.malli :as im]))

(deftest rule-test
  (is (= '[:or nil? int?] (im/rule {} '(or <nil> <int>))))
  (is (= '[:and vector?
           [:catn [a any?] [b any?] [c any?]]]
         (im/rule {} '[a b c]))))
