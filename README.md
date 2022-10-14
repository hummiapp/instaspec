# Instaspec

Specify and parse data in easy-mode.

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
(def hiccup-parser
  (is/parser
    '[hiccup (or tree literal)
      tree [tag attrs? hiccup*]
      literal (or nil? boolean? number? string?)
      tag keyword?
      attrs map?]))
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

Parse some hiccup:

```clojure
(hiccup-parser svg-data)
;=>
{tag     :svg,
 attrs?  {:viewBox [0 0 10 10]},
 hiccup* ["hello world!"
          {tag     :g,
           attrs?  nil,
           hiccup* [{tag     :circle,
                     attrs?  {:cx 1, :cy 2},
                     hiccup* []}
                    {tag     :rect,
                     attrs?  {:width 2, :height 3},
                     hiccup* []}]}]}
```

### Transforming

Intaspec creates bindings based upon the grammar definition so that we can conveniently transform the parse results into the desired output:

```clojure
;; TBD
(is/match hiccup-parser svg-data t)
;=>
100
```

Define a function:

```clojure
;; TBD
(is/defn [props? not-props] ...)
```

## Rationale

Sequence specifications are clearest when kept separate from predicates.
EBNF provides clearer sequence expressions than RegEx.
EBNF names important parts of the grammar, which is useful for both parsing and processing.

[Spec](https://clojure.github.io/spec.alpha/) (Hickey)
and similar libraries implement s-expression based RegEx interfaces to specify and parse data.
These libraries are powerful.

[Instaparse](https://github.com/Engelberg/instaparse) (Engelberg)
is easy to use.
But it parses text, not data.
Much of the convenience is due to a superior interface:
users specify grammars in EBNF rather than s-expressions.

**Instaspec** seeks to provide the convenience of Instaparse for data parsing by translating EBNF style grammar into popular data specification libraries.

[Meander](https://github.com/noprompt/meander) (Holdbrooks)
shows that substitution expressions are an expressive way to construct outputs from parsed inputs.
Other libraries tend to leave it up to the user to figure out ways to process what was parsed.

**Instaspec** seeks to provide a convenient abstraction for traversing and processing an AST based upon the names used to construct the EBNF grammar.

### Problem

1. Existing data parsing libraries conflate sequence parsing with predicates and named value capture.
Expressions are deeply nested annotations that correctly define the objective, but are inscrutable.
The user interface has been a barrier to adoption of these powerful libraries.
2. Beyond specifying and parsing, the user still has the job of processing the data. Expressing this processing often leads to repetition as it requires a custom tree traversal implementation of the same structure already specified.

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

Can we have a similar syntax in Clojure?
```clojure
(def tree
  (grammar.
    tree [tag attrs? child*]
    tag keyword?
    attrs map?
    child (or string? tree)))
(is/defn dfs [tree] 
  (prn tag))
```

### Limitations

The S-expression Regex interface was selected by library authors for good reasons.
S-expressions allow libraries flexibility beyond what EBNF can express.
Instaspec cannot expose the full range of capabilities that data parsing libraries have.
Even so, the subset of capabilities that it does expose is substantial and useful.

Instaspec addresses only matching and transformation.
Generation of values can be achieved using the underlying libraries.
It might be a good idea to add generation to the interface for convenience.

### References

Many of the aforementioned parsing libraries draw inspiration from
[General Parser Combinators in Racket (gll)](https://epsil.github.io/gll/) (Vegard Øye)

The rationale for sequence expressions is explained in
[Illuminated Macros talk](https://www.youtube.com/watch?v=o75g9ZRoLaw) (Chris Houser, Jonathan Claggett)

The rationale for specifications is explained in
[Spec rationale](https://clojure.org/about/spec) (Rich Hickey)
