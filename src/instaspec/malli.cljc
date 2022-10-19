(ns instaspec.malli
  (:require
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [malli.core :as ma]
    [malli.error :as merror]))

(declare rule)

(def nesting-prefix "<<-")

(def seq-ops
  {\? :?
   \* :*
   \+ :+})

(defn seq-op [rules x]
  (and (symbol? x)
       (let [nm (name x)
             op (-> (last nm) (seq-ops))
             nm (subs nm 0 (dec (count nm)))]
         (and op
              (seq nm)
              [(symbol nm) [op [:schema (rule rules (symbol nm))]]]))))

;; QUESTION: What is the difference between altn and orn? There doesn't seem to be any
(defn alt [rules x]
  (and (sequential? x)
       (= 'or (first x))
       [(gensym nesting-prefix) (into [:altn]
                                      (map (fn [y]
                                             [y [:schema (rule rules y)]])
                                           (rest x)))]))

(defn regex? [x]
  (instance? #?(:clj java.util.regex.Pattern
                :cljs js/RegExp)
             x))

(defn seqex [rules xs]
  (into [:catn]
        (map (fn [x]
               (or (seq-op rules x)
                   (alt rules x)
                   (and (symbol? x) [x [:schema (rule rules x)]])
                   [(gensym nesting-prefix) (rule rules x)]))
             xs)))

(defn mapex [x]
  ;; TODO: handle key names and map-of predicates
  'map?)

(defn setex [x]
  (if (seq x)
    '[:set-of (if (next x)
                (into [:and] x)
                x)]
    'set?))

;; should this include more stuff? (or less... maybe we don't need and)
(def ops '#{or and})

(defn oprule [rules more k]
  (into [k]
        (map (fn [y]
               (let [r (rule rules y)]
                 (if (or (not= k :or) (symbol? r))
                   [:orn [y r]]
                   r)))
             more)))

(defn pred? [x]
  (and (symbol? x)
       (str/starts-with? (str x) "<")
       (str/ends-with? (str x) ">")))

;; TODO: should also check the default registry (with the option to add stuff)
(defn predex [x]
  (let [s (str x)]
    (-> (subs (str x) 1 (dec (count s)))
      (str "?")
      (symbol))))

(defn rule [rules x]
  (cond (pred? x) (predex x)
        (regex? x) [:re x]
        (map? x) (mapex x)
        (set? x) (setex x)
        (vector? x) [:and 'vector? (seqex rules x)]
        (sequential? x) (or (some->> (first x) (get ops) (keyword)
                                     (oprule rules (rest x)))
                            (seqex rules x))
        (symbol? x) (if (get rules x)
                      [:ref (str x)]
                      'any?)
        :else [:= x]))

(defn registry [rules]
  (-> rules
    (update-keys str)
    (update-vals #(rule rules %))))

(defn schema [rules]
  {:pre [(< (count rules) 9)]}
  ;; TODO: ffirst only works for maps of length < 9, beyond that start must be nominated
  (let [start (ffirst rules)]
    [:schema {:registry (registry rules)}
     (str start)]))

(defn lift-nested-names
  "Malli doesn't always put the names where we want them...
  Find cases of altn and nested sequences that need to be raised up a level."
  [x]
  (cond
    (vector? x) (update x 1 lift-nested-names)
    (map? x) (persistent!
               (reduce
                 (fn [acc [k v]]
                   (if (str/starts-with? (str k) nesting-prefix)
                     (-> acc (conj! (lift-nested-names v)) (dissoc! k))
                     (assoc! acc k (lift-nested-names v))))
                 (transient {})
                 x))
    :else x))

(defn parser [rules]
  (let [s (schema rules)
        p (ma/parser s)]
    (fn parse [data]
      (let [result (p data)]
        (if (= result ::ma/invalid)
          (do
            (println "ERROR:" (-> (ma/explain s data) (merror/humanize)))
            (pprint/pprint (ma/explain s data))
            (pprint/pprint s))
          (lift-nested-names result))))))

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
