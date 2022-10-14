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
             (map #([% [:schema (rule %)]])
                  (rest x)))))

(defn regex? [x]
  (instance? #?(:clj java.util.regex.Pattern
                :cljs js/RegExp)
             x))

(defn rule [x]
  (cond (vector? x) (into [:catn]
                          (map #(or (seq-op %)
                                    (alt %)
                                    (and (symbol? %)
                                         [% [:schema (rule %)]])
                                    ;; TODO: gensym?
                                    ['_ (rule %)])
                               x))
        (list? x) (let [k (keyword (first x))]
                    (into [k]
                          (map (fn [x']
                                 (let [r (rule x')]
                                   (if (and (= k :or) (not (symbol? r)))
                                     [:orn [x' r]]
                                     r)))
                               (rest x))))
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

(defn match [p x expr]
  {:pre []}
  (let [m (p x)]
    ))
