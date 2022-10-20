(ns instaspec.core-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [instaspec.test.helper :as h]))

(use-fixtures :each h/monotone)

(def defn-args-simple
  '[foo "documentation"
    [x y]
    (+ x y)])

(def defn-args-complete
  '[my-fn "the best fn"
    {:best true} [x y]
    {:pre [(and x y)]}
    (+ x y)])

(def defn-args-multiarity
  '[my-fn
    ([x y] (+ x y))
    ([x y z] (+ x y z))])

(def defn-single-grammar
  '{defn [name doc-string? attr-map? [params*] prepost-map? body]})

(deftest defn-single-test
  (h/is-parsed defn-single-grammar defn-args-simple
               '{name        foo,
                 doc-string  "documentation",
                 attr-map    nil,
                 params      [x y],
                 prepost-map nil,
                 body        (+ x y)})
  (h/is-parsed defn-single-grammar defn-args-complete
               '{name        my-fn
                 doc-string  "the best fn"
                 attr-map    {:best true}
                 params      [x y]
                 prepost-map {:pre [(and x y)]}
                 body        (+ x y)}))

(def defn-single-and-multi-grammar
  '{defn (or [name doc-string? attr-map? [params*] prepost-map? body]
             [name doc-string? attr-map? ([params*] prepost-map? body) + post-attr-map?])})

(deftest defn-single-and-multi-test
  ;; TODO: handle separate +
  (h/is-parsed defn-single-and-multi-grammar defn-args-simple
               '{attr-map    nil
                 body        (+ x y)
                 doc-string  "documentation"
                 name        foo
                 params      [x y]
                 prepost-map nil})
  (h/is-parsed defn-single-and-multi-grammar defn-args-complete
               '{attr-map    {:best true}
                 body        (+ x y)
                 doc-string  "the best fn"
                 name        my-fn
                 params      [x y]
                 prepost-map {:pre [(and x y)]}})
  (h/is-parsed defn-single-and-multi-grammar defn-args-multiarity
               {}))

(def unconnected-grammar
  '{S [a b c]
    d <int>})

(def defn-shared-structure-grammar
  '{defn       [name doc-string? attr-map? (or arity arities)]
    arity      ([params*] prepost-map? body)
    arity-list (arity)
    arities    (arity-list+ post-attr-map?)})

(deftest defn-shared-structure-test
  (h/is-parsed defn-shared-structure-grammar defn-args-simple
               '{arity      {body        (+ x y)
                             params      {x y}
                             prepost-map nil}
                 attr-map   nil
                 doc-string "documentation"
                 name       foo})
  (h/is-parsed defn-single-and-multi-grammar defn-args-complete
                 '{attr-map    {:best true}
                   body        (+ x y)
                   doc-string  "the best fn"
                   name        my-fn
                   params      {x y}
                   prepost-map {:pre [(and x y)]}})
  (h/is-parsed defn-single-and-multi-grammar defn-args-multiarity
                 {}))

(def ^{:doc "Not supported (yet)"}
  defn-is4
  '[defn = [name doc-string? meta-map? (arity | (arity) + post-meta-map?)]
    arity = [params*] prepost-map? body])

(def ^{:doc "Shared structures"}
  defn-is3
  '{defn          [name doc? metadata? (or arity arities)]
    doc           <string>
    metadata      <map>
    arity         ([params*] prepost? body)
    ;; should it be params
    ;; params [param*] ???
    ;; TODO: every? also what's a binding spec?
    ;;params        <bindings>
    prepost       <map>
    arities       ((arity) + post-metadata?)
    post-metadata <map>})

'[defn '{arity   ([params*] prepost-map? body)
         arities ((arity) + attr-map?)}
  [name doc-string? attr-map? (or arity arities)]]
;;'#:clojure{defn #rule[:a]}

(deftest testinit

  )
