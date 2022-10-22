(ns instaspec.basic-restructuring-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [instaspec.malli :as ism]))

;; {a 1, b 2, c 3} is a sequence
;; But, sequence might also just be a vector :(
;; children is special
(def labeled-result
  '[tree {tag      :html
          children [[literal 2]
                    [literal 3]
                    [tree {tag      :body
                           children [[literal 4]]}]]}])

;; TODO: should be able to parse and translate with the same grammar in and out
(def identi
  '{
    ;; not needed? (should be ignored if present)
    ;;node     (or literal tree)
    tree [tag children]
    ;; not needed? (should be ignored if present)
    children node*
    ;;literal  (or <int> <string>)
    })

(def ts
  (tree-seq vector? (comp 'children second) labeled-result))

(declare process-node)


(deftest tt1
  (is (= [:html 2 3 :body 4]
         (ism/process-node identi labeled-result))))

(def g
  '{tree    (tag children*)
    ;; remove me
    literal ()})


;; TODO: use rewrite instead
(let [fns {'tree (fn [tag children])}])
(defn start [x]
  (let [[label y] x]

    (case label
      'tree
      'literal)
    )
  )

;; After parsing data we want to do something with the AST!
;; TASKS:
;;   count how many literals appear
;;   change all literals into spans (return as hiccup)
;;   remove literals
(deftest ttt
  ())
