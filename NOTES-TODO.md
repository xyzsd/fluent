
Notes, TODO list, and Roadmap
=============================

** term test / add more param


Notes
-----
* Now targetting JDK23



Roadmap 
-------
* Parser
    * MemorySegment vs. byte[] array
    * Consider eliminating Stream class, and only using static methods
       * all methods would require (byte[], int offset)
    * SIMD optimizations

* Rendering Performance 
    * Measurements: first, we need good benchmarks (JMH?) for message rendering
        * investigate caching/cache performance
        * target common data types (String, Numbers, lists)
        * target common functions (plural selection, temporal/date/time)
    * potentially consider, depending upon measurements: 
      * Usage of StringConcatFactory and LambdaMetafactory, vs. bytecode generation
      * Perhaps only for commonly used entries, privately cached per-bundle
    * Usage-site code generation (perhaps using classfile API)
      * create methods for message entries  
      * what about parameter typing? 
    * Convert FTL AST to bytecode (for a given resource) (AOT vs. JIT)
    


----

  * 
  * stubs for messages ? 
    https://github.com/mtumilowicz/java9-string-concat?tab=readme-ov-file
    https://kotlinlang.org/docs/whatsnew1520.html#string-concatenation-via-invokedynamic
  * better pre-sizing of StringBuilders ? keep track of size in pattern element? even if only approximate
  




    * wiki/readme for:
        - why Fluent vs. ICU::MF or MF2
        - list support & why that's important
        - example notes
        - feature highlights
        - explicitly note dependencies (just one)
            - why depends on ICU
                   1) plurals
                   2) number formatter w/skeletons
        - work on readme.md ****
           - thread safety
            FFR is threadsafe and shareable
            cache : not, but could be (if so written)
            functions : are, but if written to not be, must take precaustions
            coudl have more than one bundle for same lang (per thread); most of the work (BEFORE rendering) is
                in parsing FTL to create the FluentResource

    * OSSRH release
    * release notification on reddit / also to fluent group (GH project) AFTER OSSRH release




Someone might choose Mozilla Fluent over ICU MessageFormat 2 (MF2) due to its design focus on readability, robustness, and empowering translators to create more natural-sounding translations without being constrained by the source language.

Key Differentiators

    Asymmetric Localization (Fluent's Core Concept):

        Fluent's primary innovation is asymmetric localization. This means the translation logic in the target language is independent of the source language (like English). A simple English string can map to a complex translation in a language that requires variations based on grammar, gender, or plurals (e.g., Russian or Arabic).

This removes the burden on developers to predict and account for complex linguistic rules globally, keeping the source code simple and giving translators full expressive power.

Syntax and Readability:

    Fluent's syntax (called FTL - Fluent Translation List) is designed to be readable and editable by non-technical people (translators). It draws inspiration from simple formats like TOML and CSS.

Traditional MessageFormat syntax (and by extension, the data model MF2 represents) can be complex and prone to errors when edited by hand.

Error Recovery and Robustness:

    Fluent is designed with a strong focus on error recovery (The Robustness Principle). If a translator introduces an error in a single message, it is typically designed to degrade gracefully (e.g., render a fallback) rather than breaking the entire message or resource file.

    MF2 is a more strict format, and an error could have a higher-cost consequence, potentially resulting in a missing string in the product.

Scope and Structure:

    Fluent operates on a per-resource basis, using its single FTL syntax to write lists of messages, along with features like terms (reusable text/branding) and comments attached to messages. This helps in managing a localization resource holistically.

MF2 is defined on the level of a single message, often requiring a separate container format (like JSON or XML) to store a collection of messages, which can complicate hand-editing and double-parsing.

In essence, Fluent is an opinionated, end-to-end system that prioritizes a translator-first experience and natural-sounding results across diverse languages, even if it might mean a slight deviation from a universal data model like MF2.













* SIMD
    * note performance pitfalls
    * code improvements
        * SIMD blend overkill for skipBlankBlock
    * CRLF detection: determine best approach: load 2 vectors vs. shifting masks.
        also performance tests with CRLF line endings
    * doc notes: scalar first, simd optional (scalar fallback).
        SIMD not always faster, and much slower startup.

* FEATURE HIGHLIGHTS
    - experimental SIMD ...
    - list handling
    - JDK21 compatible






