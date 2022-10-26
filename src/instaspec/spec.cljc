(ns instaspec.spec
  (:require [clojure.spec.alpha :as s]))

(defn seq-op [x]
  (and (symbol? x)
       (let [nm (name x)
             op (-> (last nm)
                  {\? `s/?
                   \* `s/*
                   \+ `s/+})
             nm (subs nm 0 (dec (count nm)))]
         (and op
              (> (count nm) 0)
              [x (op nm)]))))

(defn alt [x]
  (and (list? x)
       (= 'or (first x))
       (into (s/alt)
             (for [x' (rest x)]
               [x' (str x')]))))

(defn rule [x]
  (cond (vector? x) (into (s/cat)
                          (for [x' x]
                            (or (seq-op x')
                                (alt x')
                                [x' (str x')])))
        (list? x) (list* (first x)
                         (for [x' (rest x)]
                           (or #_(resolve x')
                               (str x'))))
        :else (or #_(resolve x)
                  x)))

(defn register! [ns rules]
  ;; sort them

  ;; register them
  (doseq [[nm body] (partition 2 rules)]
    (let [k (keyword ns (str nm))]
      (s/def k (rule body)))))

(defn parser [rules]
  {:pre [(seq rules) (even? (count rules))]}
  (let [ns "instaspec"
        [start] rules
        start-k (keyword ns (str start))]
    (register! ns rules)
    (fn parse [x]
      (s/conform start-k x))))

#_(parser '[a int?])
