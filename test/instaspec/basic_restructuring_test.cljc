(ns instaspec.basic-restructuring-test
  (:require [clojure.test :refer [deftest is]]
            [instaspec.malli :as ism]))

(def hiccup-grammar
  '{node     (or literal tree)
    tree     [tag children*]
    children node
    tag      <keyword>
    literal  (or <int> <string>)})

(def parse-hiccup (ism/parser hiccup-grammar))

(def svg-data
  '[:html 2 3 [:body 4]])

(def ast
  '[tree {tag       :html
          children* [[literal 2]
                     [literal 3]
                     [tree {tag       :body
                            children* [[literal 4]]}]]}])

(deftest ast-test
  (is (= ast (parse-hiccup svg-data))))

(deftest traversing-test
  (is (= 5
         (count (tree-seq vector? (comp 'children* second) ast)))))

(deftest identity-test
  (is (= svg-data
         (ism/rewrite '{tree     [tag children*]
                        literal  :id}
                      ast))))

(deftest flatten-test
  (is (= [:html 2 3 :body 4]
         (ism/rewrite '{tree    (tag children*)
                        literal :id}
                      ast))))

(deftest remove-literals-test
  (is (= '(:html :body)
         (ism/rewrite '{tree    (tag children*)
                        literal ()}
                      ast))))

;; After parsing data we want to do something with the AST!
;; TASKS:
;;   count how many literals appear
;;   change all literals into spans (return as hiccup)
;;   remove literals

#_(deftest flatten-alternative-test
  (let [grammar '{node     (or literal tree)
                  tree     [tag children]
                  children node*
                  literal  (or <int> <string>)}
        ast ((ism/parser grammar) svg-data)]
    (prn ast)
    (is (= [:html 2 3 :body 4]
           (ism/rewrite '{tree     [tag children]
                          node     ()
                          children node*
                          literal  ()}
                        ast)))))
