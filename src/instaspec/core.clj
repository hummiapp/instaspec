(ns instaspec.core
  (:require
    [clojure.pprint :as pprint]
    [malli.core :as ma]))

(defn seq-op [x]
  (and (symbol? x)
       (let [nm (name x)
             op (-> (last nm)
                  {\? :?
                   \* :*
                   \+ :+})
             nm (subs nm 0 (dec (count nm)))]
         (and op
              (> (count nm) 0)
              [x [op [:schema [:ref nm]]]]))))

(defn alt [x]
  (and (list? x)
       (= 'or (first x))
       (into [:altn] (for [x' (rest x)]
                       [x' [:ref (str x')]]))))

(defn rule-malli [x]
  (cond (vector? x) (into [:catn]
                          (for [x' x]
                            (or (seq-op x')
                                (alt x')
                                [x' [:schema [:ref (str x')]]])))
        (list? x) (into [(keyword (first x))]
                        (for [x' (rest x)]
                          (if (resolve x')
                            x'
                            [:ref (str x')])))
        :else x))

(rule-malli '(or nil?))
(rule-malli '[a b c])

(defmacro grammar. [startk startv & bindings]
  (let [rules (into {startk startv}
                    (for [[k v] (partition 2 bindings)]
                      [k v]))
        malli-registry (-> rules
                         (update-keys str)
                         (update-vals rule-malli))
        malli-schema [:schema {:registry malli-registry} (str startk)]]
    {:pre [(even? (count bindings))]}
    `'{:start        ~startk
       :rules        ~rules
       :malli-schema ~malli-schema
       :malli-parser ~(ma/parser malli-schema)}))

(macroexpand '(grammar. hiccup (or tree literal)
                        tree [tag attrs? child*]
                        literal (or nil? boolean? number? string?)
                        tag keyword?
                        attrs map?
                        child (or literal tree)))

(defmacro match [grammar x r]
  `((:malli-parser ~grammar) ~x))

(defmacro isfn [fn-name args & body]
  '...
  )

(defn destructure' [bindings]
  (binding [*out* *err*] (prn "HELELLOOO")))

(with-redefs [clojure.core/destructure destructure']
  (macroexpand
    '(let [{:keys [a]} {:a 1}]
       a)))

(def hiccup
  (grammar. hiccup (or tree literal)
            tree [tag attrs? hiccup*]
            literal (or nil? boolean? number? string?)
            tag keyword?
            attrs map?))

(match hiccup
       [:svg {:viewBox [0 0, 10 10]}
        "hello world!"
        [:g
         [:circle {:cx 1, :cy 2}]
         [:rect {:width 2, :height 3}]]]
       hiccup)
