(ns instaspec.malli
  (:require
    [clojure.string :as str]
    [malli.core :as ma]
    [malli.error :as me]
    [malli.generator :as mg]))

(defn ^:dynamic *parse-fail* [schema data]
  (let [exp (ma/explain schema data)]
    (throw (ex-info (str (-> exp me/with-spell-checking me/humanize) \newline
                         (-> exp (me/error-value {::me/mask-valid-values '...})) \newline)
                    exp))))

(def nesting-prefix "<<-")
(def ^:private ^:dynamic *in-a-seqex* false)

(def seq-ops
  {\? :?
   \* :*
   \+ :+})

(declare rule)

(defn alt? [x]
  (and (sequential? x)
       (= 'or (first x))))

(defn alt
  "`alt` will inline subsequence groups (*, +, cat), unlike `or` which will create a subsequence"
  [rules x]
  (and (alt? x)
       [(gensym nesting-prefix) (into [:altn]
                                      (map (fn [y]
                                             [y (rule rules y)])
                                           (rest x)))]))

(defn seq-op? [x]
  (and (symbol? x)
       (let [nm (name x)
             op (-> (last nm) (seq-ops))
             nm (subs nm 0 (dec (count nm)))]
         (and op
              (seq nm)
              [op (symbol nm)]))))

(defn seq-op [rules x]
  (or (alt rules x)
      (when-let [[op sn] (seq-op? x)]
        [x [op (rule rules sn)]])))

(defn regex? [x]
  (instance? #?(:clj java.util.regex.Pattern
                :cljs js/RegExp)
             x))

(defn seqex [rules xs]
  (binding [*in-a-seqex* true]
    (cond->>
      (into [:catn]
            (map (fn [x]
                   (or (seq-op rules x)
                       (and (symbol? x) [x (rule rules x)])
                       ;; TODO: a literal isn't really nested, just ignored
                       [(gensym nesting-prefix) (rule rules x)]))
                 xs))
      (<= (count xs) 1)
      (conj [:schema]))))

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
;; TODO: there is redundancy with seq-ops
(def ops
  '{or  :or
    and :and
    +   :+
    *   :*
    ?   :?})

(defn pred? [x]
  (and (symbol? x)
       (str/starts-with? (str x) "<")
       (str/ends-with? (str x) ">")))

(defn name? [x]
  (and (symbol? x) (not (pred? x))))

(defn oprule [rules more op]
  (into [op] (map (fn [arg]
                    (let [r (rule rules arg)]
                      (if (name? arg)
                        [:orn [arg r]]
                        r)))
                  more)))

;; TODO: should also check the default registry (with the option to add stuff)
(defn predex [x]
  (let [s (str x)]
    (-> (subs (str x) 1 (dec (count s)))
      (str "?")
      (symbol))))

(defn inline? [rules x]
  (and *in-a-seqex*
       (or (alt? x)
           (seq-op? x)
           (sequential? x)
           (and (name? x)
                (let [r (get rules x)]
                  (recur rules r))))))

(defn rule [rules x]
  (cond (pred? x) (predex x)
        (regex? x) [:re x]
        (map? x) (mapex x)
        (set? x) (setex x)
        (vector? x) [:and 'vector? (seqex rules x)]
        (sequential? x) (or (some->> (first x) (get ops)
                                     (oprule rules (rest x)))
                            (seqex rules x))
        (name? x) (if-let [r (get rules x)]
                    (if (inline? rules r)
                      (str x)
                      [:schema [:ref (str x)]])
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

;; TODO: this is not explicit enough
;; a 2 vector of symbols can have a name collision with the grammar
(defn named-pair? [rules x]
  (and (vector? x)
       (= 2 (count x))
       (let [nm (first x)]
         (and (symbol? nm)
              (contains? rules nm)))))

(defn lift-nested-names
  "Malli doesn't always put the names where we want them...
  Find cases of altn and nested sequences that need to be raised up a level."
  [rules x]
  (cond
    (named-pair? rules x) (let [[k v] x]
                            [k (lift-nested-names rules v)])
    (vector? x) (mapv #(lift-nested-names rules %) x)
    (map? x) (persistent!
               (reduce
                 (fn [acc [k v]]
                   (if (str/starts-with? (str k) nesting-prefix)
                     (-> acc
                       (dissoc! k)
                       (cond-> (or (map? v)
                                   (named-pair? rules v))
                         (conj! (lift-nested-names rules v))))
                     (assoc! acc k (lift-nested-names rules v))))
                 (transient {})
                 x))
    :else x))

(defn parser [rules]
  (let [s (schema rules)
        p (ma/parser s)]
    (fn parse [data]
      (let [result (p data)]
        (if (= result ::ma/invalid)
          (*parse-fail* s data)
          (lift-nested-names rules result))))))

(defn validator [rules]
  (let [s (schema rules)
        v (ma/validator s)]
    (fn valid? [data]
      (let [result (v data)]
        (if (= result ::ma/invalid)
          (*parse-fail* s data)
          (lift-nested-names rules result))))))

;; TODO: most grammars fail to generate data
(defn generate [rules]
  (let [s (schema rules)]
    (mg/generate s)))

(defn *? [x]
  (and (symbol? x)
       (str/ends-with? (str x) "*")))

(declare rewrite)

(defn apply-rule [grammar rule x]
  (cond (vector? rule) (vec (mapcat #(let [y (apply-rule grammar % x)]
                                       (if (sequential? y)
                                         y
                                         [y]))
                                    rule))
        (sequential? rule) (doall (mapcat #(let [y (apply-rule grammar % x)]
                                             (if (sequential? y)
                                               (flatten y)
                                               [y]))
                                          rule))
        (seq-op? rule) (rewrite grammar [rule (get x rule)])
        (name? rule) (let [y (get x rule)
                               r (get grammar rule)]
                           (cond (*? r)
                                 (vec
                                   (mapcat #(let [z (rewrite grammar %)]
                                              (if (sequential? z)
                                                (flatten z)
                                                [z]))
                                           y))
                                 (contains? grammar r)
                                 (rewrite grammar y)
                                 :else y))
        (nil? rule) ()
        :else x))

(defn rewrite [grammar ast]
  (let [[tag x] ast]
    (let [rule (get grammar tag)]
      (if (*? tag)
        (mapv #(rewrite grammar %) x)
        (apply-rule grammar rule x)))))
