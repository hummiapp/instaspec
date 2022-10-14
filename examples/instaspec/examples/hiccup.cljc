(ns instaspec.examples.hiccup
  (:require
    [clojure.pprint :as pprint]
    [instaspec.core :as is]
    [instaspec.malli :as im]))

(def hiccup-ebnf
  '[element (or literal tree)
    tree (and vector? [tag attrs? element*])
    literal (or nil? boolean? number? string?)
    tag keyword?
    attrs (map-of keyword? any?)])

(println "** HICCUP EBNF **")
(pprint/pprint
  (im/registry hiccup-ebnf))

(def hiccup-parser
  (im/parser hiccup-ebnf))

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

(def z '{element ()
         ;literal identity
         tree    (fn [acc {:keys [tag attrs? element*]}]
                   `[~(if (= :circle tag) :rect tag)
                     ~@(if attrs? [attrs?] [])
                     ~@(map element element*)])
         tag     ()

         })

;; don't want this, it should just dispatch literal and tree
(defn element [acc [sub-type v]]
  (if (= sub-type 'tree)
    (tree v)
    v))

(defn tree [acc {:keys [tag attrs? element*]}]
  `[~(if (= :circle tag)
       :rect tag)
    ~@(if attrs? [attrs?] [])
    ~@(map element element*)])


(defmulti polygonize 'tag)
(defmethod polygonize :ellipse)

(defmulti depolygonize 'tag)
(defmethod depolygonize :ellipse)

(defmulti interpolate 'tag)
(defmethod interpolate [:rect :ellipse])
(defmethod interpolate [:square :rect])
(defmethod interpolate [:circle :triangle])
;; Generally n-ary polygons with curvature?
(defmethod interpolate [:polygon :polygon])

(im/dfs hiccup-ebnf z)

;; need to decide which matched...
#_(im/matcher hiccup-ebnf
            (fn [{:keys [tag attrs? element*]}]
              (cons tag (recur element*))))

;; would like a substitution pattern
#_(im/match hiccup-parser
          '[element (or [literal] tree)
            tree [tag element*]])
