(ns instaspec.core-test
  (:require
    [clojure.pprint :as pprint]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [instaspec.core :as is]
    [instaspec.malli :as ism]))

(use-fixtures :each
  (fn [test]
    (let [n (atom 0)]
      (with-redefs [gensym (fn [prefix]
                             (symbol (str prefix (swap! n inc))))]
        (test)))))

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
  '[my-fn ([x y] (+ x y)) ([x y z] (+ x y z))])

(defn report [label x]
  (println)
  (println "*** " label " ***")
  (pprint/pprint x))

(defmacro is-parsed [grammar data expected]
  `(let [s# (ism/schema ~grammar)
         _# (report "Schema" s#)
         p# (ism/parser ~grammar)
         r# (p# ~data)]
     (report "Result" r#)
     (is (= ~expected r#))))

(def defn-single-grammar
  '{defn [name doc-string? attr-map? [params*] prepost-map? body]})

(deftest defn-single-test
  (is-parsed defn-single-grammar defn-args-simple
             '{name        foo,
               doc-string  "documentation",
               attr-map    nil,
               ;; TODO: notice that params are inaccessibly hidden under _
               _is2        {params [x y]},
               prepost-map nil,
               body        (+ x y)})
  (is-parsed defn-single-grammar defn-args-complete
             '{name        my-fn
               doc-string  "the best fn"
               attr-map    {:best true}
               _is4        {params [x y]}
               prepost-map {:pre [(and x y)]}
               body        (+ x y)}))

(def defn-single-and-multi-grammar
  '{defn (or [name doc-string? attr-map? [params*] prepost-map? body]
             [name doc-string? attr-map? ([params*] prepost-map? body) + post-attr-map?])})

(deftest defn-single-and-multi-test
  ;; TODO: handle separate +
  (is-parsed defn-single-and-multi-grammar defn-args-simple
             {})
  (is-parsed defn-single-and-multi-grammar defn-args-complete
             {})
  (is-parsed defn-single-and-multi-grammar defn-args-multiarity
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
  (is-parsed defn-shared-structure-grammar defn-args-simple
             {})
  (is-parsed defn-single-and-multi-grammar defn-args-complete
             {})
  (is-parsed defn-single-and-multi-grammar defn-args-multiarity
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
