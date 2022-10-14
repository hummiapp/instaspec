(ns instaspec.spec-test
  (:require [clojure.test :refer [deftest is testing]]
            [instaspec.spec :as is]))

(deftest rule-test
  (is (= (or nil?) (is/rule '(or nil?))))
  (is (= () (is/rule '[a b c]))))
