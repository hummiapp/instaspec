(ns instaspec.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [instaspec.core :as is]))

(defn foo {:m 1}
  ([x])
  {:bar :baz})

(def ^{:doc "Basic syntactic sugar"}
  defn-is1
  '{defn [name doc-string? attr-map? [params*] prepost-map? body]})

(def ^{:doc "Two signatures"}
  defn-is2
  '{defn (or [name doc-string? attr-map? [params*] prepost-map? body]
             [name doc-string? attr-map? ([params*] prepost-map? body) + attr-map?])})

(def ^{:doc "Shared structures"}
  defn-is3
  '{defn    [name doc-string? attr-map? (or arity arities)]
    arity   ([params*] prepost-map? body)
    arities ((arity) + attr-map?)})

(def ^{:doc "Not supported (yet)"}
  defn-is4
  '[defn = [name doc-string? meta-map? (arity | (arity) + post-meta-map?)]
    arity = [params*] prepost-map? body])

(def ^{:doc "Shared structures"}
  defn-is3
  '{defn    [name doc-string? attr-map? (or arity arities)]
    arity   ([params*] prepost-map? body)
    arities ((arity) + attr-map?)})

'[defn '{arity   ([params*] prepost-map? body)
         arities ((arity) + attr-map?)}
  [name doc-string? attr-map? (or arity arities)]]
;;'#:clojure{defn #rule[:a]}

(deftest testinit

  )
