(ns instaspec.examples.hiccup
  (:require
    [clojure.pprint :as pprint]
    [instaspec.malli :as is]))

(def hiccup-is
  '{element (or literal tree)
    tree    (and vector? [tag attrs? element*])
    literal (or nil? boolean? number? string?)
    tag     keyword?
    attrs   (map-of keyword? any?)})

(println "** HICCUP Registry **")
(pprint/pprint
  (is/registry hiccup-is))

(def hiccup-parser
  (is/parser hiccup-is))

(def svg-data
  [:svg {:viewBox [0 0, 10 10]}
   "hello world!"
   [:g
    [:circle {:cx 1, :cy 2}]
    [:rect {:width 2, :height 3}]]])

(println "** PARSE **")
(pprint/pprint
  (hiccup-parser svg-data))

;; create test cases
;; compelling example(s)
;; * interpolate
;; hoisting tag and attrs? was useful (they aren't children like in instaparse)
;; substitute by name
;; results will either be a vector (a rule) or a map (cat rules)
;; translate etc need to be nested/searchable/updatable
;; need a better splicing syntax
;; need (mutual) recursion
;; kinda like a ns?
;; can I just use *this* ns?
;; Should I have control over the traversal?
;; - could rely on bottom up, or dfs top down
;; -- a system of functions that can dispatch themselves
;; unnamed things shouldn't be named (predicates)

(println "** REWRITE **")
(pprint/pprint
  (is/rewrite
    '{tree    (and vector? [~(if (= tag :circle) :rect tag) attrs? element*])
      literal ()}
    (hiccup-parser svg-data)))
