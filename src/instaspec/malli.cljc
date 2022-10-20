(ns instaspec.malli
  (:require
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [malli.core :as ma]
    [malli.error :as me]
    [malli.generator :as mg]))

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
              [(symbol nm) [op (rule rules (symbol nm))]]))))

;; QUESTION: What is the difference between altn and orn? There doesn't seem to be any
(defn alt [rules x]
  (and (sequential? x)
       (= 'or (first x))
       [(gensym nesting-prefix) (into [:altn]
                                      (map (fn [y]
                                             [y (rule rules y)])
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
                   (and (symbol? x) [x (rule rules x)])
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
(def ops
  '{or  :or
    and :and})

(defn pred? [x]
  (and (symbol? x)
       (str/starts-with? (str x) "<")
       (str/ends-with? (str x) ">")))

(defn oprule [rules more op]
  (into [op]
        (map (fn [arg]
               (let [r (rule rules arg)]
                 (if (and (symbol? arg) (not (pred? arg)))
                   [:orn [arg r]]
                   r)))
             more)))

;; TODO: should also check the default registry (with the option to add stuff)
(defn predex [x]
  (let [s (str x)]
    (-> (subs (str x) 1 (dec (count s)))
      (str "?")
      (symbol))))

(defn recursive? [rules x]
  ;; TODO: how to detect recursive rules?
  (= 'node x)
  #_(-> (get rules x)))

(defn rule [rules x]
  (cond (pred? x) (predex x)
        (regex? x) [:re x]
        (map? x) (mapex x)
        (set? x) (setex x)
        (vector? x) [:and 'vector? (seqex rules x)]
        (sequential? x) (or (some->> (first x) (get ops)
                                     (oprule rules (rest x)))
                            (seqex rules x))
        (symbol? x) (if-let [r (get rules x)]
                      (if (recursive? rules x)
                        [:schema [:ref (str x)]]
                        (str x))
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
    ;; TODO: this is a dodgy way to distinguish between true vectors and tagged pairs
    ;; find a better way to know the difference
    (and (vector? x)
         (symbol? (first x))) (let [[k v] x]
                                {k (lift-nested-names v)})
    (vector? x) (mapv lift-nested-names x)
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
            (println "ERROR:" (-> (ma/explain s data) (me/humanize)))
            (pprint/pprint (ma/explain s data)))
          (lift-nested-names result))))))

(defn generate [rules]
  (let [s (schema rules)]
    (mg/generate s)))

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
