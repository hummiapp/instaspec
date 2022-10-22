# Instaspec

Specify, parse, and transform data in easy-mode.

Instaspec is a concise and easy rule syntax for parsing and transforming data that separates the shape of data from predicates.

Instaspec syntax imitates the comfortable interface of EBNF (ala Instaparse) to describe data.
Grammars are translated to Specs.
Transformations are facilitated with a construction helper with the same syntax.

![Hummingbird](https://hummi.app/img/hummi.svg)

## Usage

Instaspec is a Clojure(Script) library that requires either Spec or Malli to be a provided dependency.

### Adding dependencies

Choose a prerequisite dependency:

`metosin/malli {:mvn/version "0.8.9"}`

Add Instaspec as a dependency:

`app.hummi/instaspec {:mvn/version "0.0.1"}`

### Parsing

Require instaspec in your namespace:

```clojure
(ns example.core
  (:require [instaspec.malli :as is]))
```

Define a grammar:

```clojure
(def hiccup-grammar
  '{element (or literal tree)
    tree [tag attrs? element*]
    literal (or <nil> <boolean> <number> <string>)
    tag <keyword>
    attrs <map>})
```

Create a parser from the grammar:

```clojure
(def hiccup-parser
  (is/parser hiccup-grammar))
```

Given some data to parse:

```clojure
(def svg-data
  [:svg {:viewBox [0 0, 10 10]}
   "hello world!"
   [:g
    [:circle {:cx 1, :cy 2}]
    [:rect {:width 2, :height 3}]]])
```

TODO: improved syntax support coming soon...

`rewrite` is a tiny scaffold for transforming the parse results into the desired output.
You write functions named for the rules in your grammar,
and they will be called in a trasformation traversal.

```clojure
(defn tree$ [{:syms [tag attrs? element*]}]
  `[~(if (= :circle tag)
       :rect tag)
    ~@(if attrs? [attrs?] [])
    ~@(map is/rewrite element*)])
(is/rewrite (hiccup-parser svg-data))
;=>
[:svg {:viewBox [0 0 10 10]}
 "hello world!"
 [:g [:rect {:cx 1, :cy 2}] [:rect {:width 2, :height 3}]]]
```

Above we replaced the `:circle` with a `:rect`.
The naming of `tree$` is a convention that `rewrite` uses to identify the transformation for `tree`.
The trailing `$` indicates that the function is for transformation, not a predicate!

The usage of `~@` is regular Clojure templating.

Rewrite functions take as input a map of symbols from the grammar, and return a replacement.

If no rewrite function is present for a name `identity` is used instead.

TODO: would be more explicit to provide functions instead of resolving them, but would also be more versbose. Should this be an option at least? 

TODO: Aggregation can be done by closing over rewrite functions, but it might be nice to have a more friendly version of that. Perhaps everything should be an aggregation, and provide a default for replacement.

TODO: maybe provide several flavors of transformers?

TODO: Instaparse allows users to remove parts of the AST with `<>` annotation. This might be useful?

Parsing the data binds names from your grammar:

```clojure
(hiccup-parser svg-data)
;=>
[tree {tag :svg,
       attrs? {:viewBox [0 0 10 10]},
       element* [[literal "hello world!"]
                 [tree {tag :g,
                        attrs? nil,
                        element* [[tree {tag :circle,
                                         attrs? {:cx 1, :cy 2},
                                         element* []}]
      [tree {tag :rect,
             attrs? {:width 2, :height 3},
             element* []}]]}]]}]
```

TODO: Validating

TODO: Generating

### Conveniences

Define a function:

```clojure
;; TBD
(is/defn [props? not-props] ...)
```

Generate values: TODO

## Grammar

Grammars are data.
A grammar consists of name, rule pairs (similar to bindings).
Rules may contain predicates and boolean logic.
Predicates are identified by resolving the symbol.
Names occurring in sequences treat `?`, `+`, and `*` suffixes as regex operands.
Names that do not resolve are treated as bindings.

In the example:

```clojure
'{hiccup (or tree literal)
  tree [tag attrs? hiccup*]
  literal (or <nil> <boolean> <number> <string>)
  tag <keyword>
  attrs <map>}
```

* `<nil>` is a predicate (resolves to the `nil?` function)
* `attrs?` is the name for an optional value in a sequence
* `hiccup*` will create a sequence of 0 or more matches
* `or` is a special operator

## API

`is/rewrite` is the primary interface to parse and transform with.

`is/parser` creates a parser only. Parsers return a hiccup style tree.

`is/registry` builds the underlying libraries' construction.

## Rationale

Sequence specifications are clearest when kept separate from predicates.
EBNF provides clearer sequence expressions than s-expression RegEx.
EBNF decomposes grammar and names those decompositions, which is useful for both parsing and processing.

[Spec](https://clojure.github.io/spec.alpha/) (Hickey)
and similar libraries implement s-expression based RegEx interfaces to specify and parse data.
These libraries are powerful.

[Instaparse](https://github.com/Engelberg/instaparse) (Engelberg)
is easy to use.
But it parses text, not data.
Much of the convenience is due to a superior interface:
users specify grammars in EBNF rather than s-expressions.

**Instaspec** provides the convenience of Instaparse for data parsing by translating EBNF style grammar into popular data specification libraries.

[Meander](https://github.com/noprompt/meander) (Holdbrooks)
shows that substitution expressions are an expressive way to construct outputs from parsed inputs.
Other libraries tend to leave it up to the user to figure out ways to process what was parsed.

**Instaspec** provides a convenient abstraction for traversing and processing an AST based upon the names used to construct the EBNF grammar.

### Problem

1. The s-expression interface to existing data parsing libraries conflates sequence parsing with predicates and named value capture.
Expressions are deeply nested annotations that correctly define the objective, but are inscrutable.
The user interface has been a barrier to adoption of these powerful libraries.
2. Beyond specifying and parsing, the user still has the job of transforming the data. Expressing this processing often leads to repetition as it requires a custom tree traversal implementation of the same structure already specified.

### Solution

1. Decomplect sequences from predicates, and named value capture.
   Instaspec is a mapping from EBNF style grammar to Spec library s-expressions.
   EBNF consists of rules.
   The first rule a valid value in terms of other rules.
   A rule can only be a sequence, disjunction, or predicate.
   This restriction prevents complexion.
2. Provide implicit destructuring for node handling and recursion.

### Goals

#### Ease of use 

- Sequences look like the sequence: `tree [tag attr? child*]`
- Sequences only contain names and sequence features (`?`, `+`, `*`)
- Declare predicates separately: `tag keyword?`
- Create bindings for the names from the grammar

#### Augment existing libraries

- Don't try to reinvent or replace them
- Don't limit extensibility and composability
- Do try to make existing libraries easier to use with expressive sugar.

### Parsing text: Instaparse

- EBNF is clear, concise, and precise
- Instaparse just works! I can't imagine how else I'd be able to make a parser
- Predicates are literals or string regex rules
- Supports different styles of parsing
- **I wish data parsing libraries were more like Instaparse**

But not suitable for data:

- Input must be text.
- The resulting AST needs to be processed, often according to the same rules you already defined for parsing.
  So now you are back where you started: parsing again but now with data input instead of text input.
  This can be partially alleviated by using node transformations for simple nodes like numbers.

### Parsing data: Specification libraries

There are many data parsing libraries to choose from:

| Library                                                         | Primary Author |
|-----------------------------------------------------------------|----------------|
| [clojure/spec.alpha](https://github.com/clojure/spec.alpha)     | richhickey     |
| [metosin/malli.alpha](https://github.com/metosin/malli)         | ikitommi       |
| [prismatic/schema](https://github.com/plumatic/schema)          | w01fe          |
| [clojure/core.match](https://github.com/clojure/core.match)     | swannodette    |
| [noprompt/meander.epsilon](https://github.com/noprompt/meander) | noprompt       |
| [cgrand/seqexp](https://github.com/cgrand/seqexp)               | cgrand         |
| [jclaggett/seqex](https://github.com/jclaggett/seqex)           | jclaggett      |

- These libraries are high quality and powerful
- Malli and Schema implement data driven specs
- Meander has substitution, which is helpful for output transformations
- Consuming nested bindings can be a challenge in all libraries
  - Seqex allows you to explicitly consume the bindings any way you'd like to
- Seqex is very composable and extensible (it's all functions)
- Explain why a match fails is an excellent feature that most libraries provide

### Motivation: Make sequences clearer

Let's consider specification of function arguments.

Basic case: Reagent components often benefit from being able to supply optional attributes to be applied to their
node (in this case we could create another arity version for optionality):

```clojure
(defn hexagon
  "attrs must be a map, radius must be a number"
  [attrs? radius]
  [:g (merge {:stroke "green"} attrs)
   [:path {:d (make-points radius)}]])
```

Slightly more complicated case: Depth First Search over a hiccup tree. 
The sequence part should only contain sequency things.

```
tree := tag attrs? child*
tag := keyword?
attrs := map?
child := string? | tree
```


### Limitations

The S-expression Regex interface was selected by library authors for good reasons.
S-expressions allow libraries flexibility beyond what EBNF can express.
Instaspec cannot expose the full range of capabilities that data parsing libraries have.
Even so, the subset of capabilities that it does expose is substantial and useful.

### References

Many of the aforementioned parsing libraries draw inspiration from
[General Parser Combinators in Racket (gll)](https://epsil.github.io/gll/) (Vegard Øye)

The rationale for sequence expressions is explained in
[Illuminated Macros talk](https://www.youtube.com/watch?v=o75g9ZRoLaw) (Chris Houser, Jonathan Claggett)

The rationale for specifications is explained in
[Spec rationale](https://clojure.org/about/spec) (Rich Hickey)

Seqex parsing is explained in
[Structure and Interpretation of Malli Regex Schemas](https://www.metosin.fi/blog/malli-regex-schemas/) (Jaakkola)

Motivations for Malli are explained in
[Malli: Inside Data-driven Schemas](https://www.youtube.com/watch?v=MR83MhWQ61E) (Reiman)

Instaparse is explained in [Instaparse: What if context-free grammars were as easy to use as regular expressions?](https://www.youtube.com/watch?v=b2AUW6psVcE) (Engelberg)
