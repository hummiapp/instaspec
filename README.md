# Instaspec

Specify and parse data in easy-mode.

![](https://hummi.app/img/hummi.svg)

## Usage

Instaspec is a Clojure(Script) library that requires either Spec or Malli to be a provided dependency.

Choose a prerequisite dependency:

`metosin/malli {:mvn/version "0.8.9"}`

Add instaspec as a dependency:

`app.hummi/instaspec {:mvn/version "0.0.1"}`

Require instaspec in your namespace:

```clojure
(ns example.core
  (:require [instaspec.core :as is]))
```

Define a grammar:

```clojure
(def hiccup-grammar
  (is/grammar.
    hiccup (or tree literal)
    tree [tag attrs? hiccup*]
    literal (or nil? boolean? number? string?)
    tag keyword?
    attrs map?))
```

Parse some hiccup:

```clojure
(match hiccup-grammar
       [:svg {:viewBox [0 0, 10 10]}
        "hello world!"
        [:g
         [:circle {:cx 1, :cy 2}]
         [:rect {:width 2, :height 3}]]]
       hiccup)
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

Define a function:

```
(is/defn [props? not-props] ...)
```

## Rationale

[Spec](https://clojure.github.io/spec.alpha/) (Hickey)
implements an s-expression interface to specify and parse data.
The interface is, in my opinion, difficult to use.

[Instaparse](https://github.com/Engelberg/instaparse) (Engelberg)
by contrast is very easy to use.
But it only parses text.
Much of the convenience is due to a superior interface:
users specify grammars in EBNF.

**Instaspec** seeks to provide the convenience of Instaparse for data parsing with EBNF style grammar on top of popular data spec libraries.

### The Problem

Existing data parsing libraries conflate sequence parsing with predicates and named value capture.
Expressions are deeply nested annotations that correctly define the objective, but are inscrutable.
The interface has deterred adoption of these powerful libraries.

### Solution

Decomplect sequences from predicates, and named value capture by
implementing a mapping from EBNF style grammar to Spec library s-expressions.
EBNF style grammar separates these concerns in a concise and obvious syntax.

### Goals

1. Ease of use 
  - Sequences look like the sequence: `tree [tag attr? child*]`
  - Sequences only contain names and sequence features (`?`, `+`, `*`)
  - Declare predicates separately: `tag keyword?`
  - Create bindings for the names from the grammar
2. Augment existing libraries
  - Don't try to replace them
  - Don't limit their extensibility and composability
  - Do try to make them more attractive

### Parsing text: Instaparse

- EBNF is clear, concise, and precise
- Instaparse just works! I can't imagine how else I'd be able to make a parser
- Predicates are literals or string regex rules
- Has rich support for different styles of parsing
- **I wish data parsing libraries were more like Instaparse**

But not suitable for data:

- Input must be text.
- The resulting AST needs to be processed, often according to the same rules you already defined for parsing.
  So now you are back where you started: parsing again but now with data input instead of text input.
  This can be partially alleviated by using node transformations for simple nodes like numbers.

### Specification and parse libraries for data

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

S-expressions were selected by library authors with good reason.
S-expressions allow libraries to go beyond what EBNF can express.
I agree with their rationale.

Instaspec cannot expose the full capabilities that data parsing libraries have.

Instaspec can make it much easier for situations that are satisfied by EBNF, which is a substantial subset of usecases.

### References

Many of the aforementioned parsing libraries draw inspiration from
[General Parser Combinators in Racket (gll)](https://epsil.github.io/gll/) (Vegard Øye)

The rationale for sequence expressions is explained in
[Illuminated Macros talk](https://www.youtube.com/watch?v=o75g9ZRoLaw) (Chris Houser, Jonathan Claggett)

The rationale for specifications is explained in
[Spec rationale](https://clojure.org/about/spec) (Rich Hickey)
