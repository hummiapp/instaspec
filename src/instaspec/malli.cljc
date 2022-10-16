(ns instaspec.malli
  (:require [malli.core :as ma]))

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

(declare rule)

(defn alt [x]
  (and (list? x)
       (= 'or (first x))
       (into [:altn]
             (map (fn [y]
                    [y [:schema (rule y)]])
                  (rest x)))))

(defn regex? [x]
  (instance? #?(:clj java.util.regex.Pattern
                :cljs js/RegExp)
             x))

(defn seqex [x]
  (into [:catn]
        (map #(or (seq-op %)
                  (alt %)
                  (and (symbol? %)
                       [% [:schema (rule %)]])
                  ;; TODO: gensym?
                  ['_ (rule %)])
             x)))

(defn mapex [x]
  ;; TODO: handle key names and map-of predicates
  'map?)

(defn setex [x]
  (if (seq x)
    '[:set-of (if (next x)
                (into [:and] x)
                x)]
    'set?))

;; should this include more stuff?
(def ops '#{or and})

(defn oprule [k more]
  (into [k]
        (map (fn [y]
               (let [r (rule y)]
                 (if (or (not= k :or) (symbol? r))
                   [:orn [y r]]
                   r)))
             more)))

(defn rule [x]
  (cond (list? x) (or (some-> (first x) (get ops) (keyword)
                              (oprule (rest x)))
                      (seqex x))
        (vector? x) [:and 'vector? (seqex x)]
        (map? x) (mapex x)
        (set? x) (setex x)
        ;; TODO: resolve is probably not quite right...
        ;; really this is anything in the default registry (with the option to add stuff)
        (and (symbol? x) (resolve x)) x
        (symbol? x) [:ref (str x)]
        (regex? x) [:re x]
        :else [:= x]))

(defn registry [rules]
  (-> (apply hash-map rules)
    (update-keys str)
    (update-vals rule)))

(defn parser [rules]
  {:pre [(seq rules) (even? (count rules))]}
  (let [[start] rules
        schema [:schema {:registry (registry rules)} (str start)]]
    (ma/parser schema)))

;; TODO: should provide a version that only resolves once, when the parser is made
(defn rewrite [node]
  (if (vector? node)
    (let [[label value] node
          transform (resolve (symbol (str label "$")))]
      (if transform
        (transform value)
        value))
    node))

(defn rewriter [rules]
  {:pre [(seq rules) (even? (count rules))]}
  (comp rewrite (parser rules)))
