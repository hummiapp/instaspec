(ns instaspec.basic-restructuring-test
  (:require [clojure.test :refer [deftest is testing]]))

;; NO NO NO, labels should be [label ...]
;; Hint: {a 1, b 2, c 3} is a sequence!
;; But, sequence might also just be a vector :(
;; children is special
(def labeled-result
  '[tree {tag      :html
          children [[literal 2]
                    [literal 3]
                    [tree {tag      :body
                           children [[literal 4]]}]]}])

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
;;
(deftest ttt
  ())
