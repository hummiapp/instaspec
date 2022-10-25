(ns instaspec.core-test
  (:require
    [clojure.pprint :as pprint]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [instaspec.malli :as ism]
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
  '[my-fn "multiarity"
    {:better true}
    ([x y] (+ x y))
    ([x y z] (+ x y z))])

(def defn-args-multiarity-needs-types
  '[my-fn
    ([x y] (+ x y))
    ([x y z] (+ x y z))])

(def defn-single-grammar
  '{defn [name doc-string? attr-map? [params*] prepost-map? body]})

(deftest defn-single-test
  (h/is-parsed defn-single-grammar defn-args-simple
               '{name         foo,
                 doc-string?  "documentation",
                 attr-map?    nil,
                 params*      [x y],
                 prepost-map? nil,
                 body         (+ x y)})
  (h/is-parsed defn-single-grammar defn-args-complete
               '{name         my-fn
                 doc-string?  "the best fn"
                 attr-map?    {:best true}
                 params*      [x y]
                 prepost-map? {:pre [(and x y)]}
                 body         (+ x y)}))

(def defn-single-and-multi-grammar
  '{defn (or [name doc-string? attr-map? [params*] prepost-map? body]
             ;; How do you ask for a sequence vs a group??
             [name doc-string? attr-map? (+ (([params*] prepost-map? body))) post-attr-map?])})

(deftest defn-single-and-multi-test
  ;; TODO: handle separate +
  (h/is-parsed defn-single-and-multi-grammar defn-args-simple
               '{name         foo
                 doc-string?  "documentation"
                 attr-map?    nil
                 params*      [x y]
                 prepost-map? nil
                 body         (+ x y)})
  (h/is-parsed defn-single-and-multi-grammar defn-args-complete
               '{name         my-fn
                 doc-string?  "the best fn"
                 attr-map?    {:best true}
                 params*      [x y]
                 prepost-map? {:pre [(and x y)]}
                 body         (+ x y)})
  ;; params are missing in multi-arity because the subsequence is anonymous
  ;; TODO: should this be an error? warning? or get a useful name?
  (h/is-parsed defn-single-and-multi-grammar defn-args-multiarity
               '{name           my-fn
                 doc-string?    "multiarity"
                 attr-map?      {:better true}
                 post-attr-map? nil}))

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
               '{name        foo
                 doc-string? "documentation"
                 attr-map?   nil
                 arity       {params*      [x y]
                              body         (+ x y)
                              prepost-map? nil}})
  (h/is-parsed defn-shared-structure-grammar defn-args-complete
               '{name        my-fn
                 doc-string? "the best fn"
                 attr-map?   {:best true}
                 arity       {body         (+ x y)
                              params*      [x y]
                              prepost-map? {:pre [(and x y)]}}})
  (h/is-parsed defn-shared-structure-grammar defn-args-multiarity
               '{name        my-fn
                 doc-string? "multiarity"
                 attr-map?   {:better true}
                 arities     {arity-list+    [{arity {params*      [x y]
                                                      prepost-map? nil
                                                      body         (+ x y)}}
                                              {arity {params*      [x y z]
                                                      prepost-map? nil
                                                      body         (+ x y z)}}]
                              post-attr-map? nil}}))

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
    arities       ((+ (arity)) post-metadata?)
    post-metadata <map>})

'[defn '{arity   ([params*] prepost-map? body)
         arities ((arity) + attr-map?)}
  [name doc-string? attr-map? (or arity arities)]]
;;'#:clojure{defn #rule[:a]}

#_(deftest testinit

  )

(defmacro qualify [ns coll]
  '...)

'[clojure
  {defn    [name doc? attrs? (arity | arities)]
   doc     string?
   attrs   map?
   arity   ([params*] prepost-map? body)
   arities ((arity) + attr-map?)}]
