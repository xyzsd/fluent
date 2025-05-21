# Project Thoughts, Architectural Considerations, and Future Directions

# do all this under FluentNG branch

Near-term TODOs
----------------
- DONE: JDK bump
- DONE: add JSpecify as dependency
- DONE: merge ICU functions with base
- DONE: JPMS module implementation (all exported for now)
- DONE: remove old directories
- DONE: JSpecify instead of JSR305 everywhere
- Finish major refactoring
- Begin (and complete!) selector refactoring, with particular attention given to handling selection over lists and the
    variables within the variants within a selection. 
- API tuning of FluentBundle
- GOAL: preserve messages in face of adversity. FluentError preferred over Exceptions, in an attempt to better
  preserve the message. Need to flag errors/exceptions for alternative output or at least allow logging.
- jdk: 21 vs 23
- (later) JPMS Modularization accessibility: determine what shouldn't be exported (if anything)
- Logging: System.logger (no dependencies)
- TESTS TESTS TESTS particularly custom formatters/implicits
- Utility
  - easy way to read .ftl from file or resource -> stream
  - way to get file/resource names from Locale
    - 'und' or root returns "" empty string
    - no prefix/suffix
    - just like "en_US" etc.
    - preserve case BUT could make all-lowercase too
    - "prefix_" en_US ".postfix"
    - 
- language matching
  - how to search/match classpath for resource fork/language matching
      - or a group of files/urls/etc.
      - a couple of ways:
        - provide known languages ahead of time
          - string paths, 'paths', URIs, depends. associate with locale
          - then use localematcher
        - OR
        - probe (e.g., en_gb -> en -> root) (root or fallback)
        - via constructing an array of names: 
              'en_gb_xx' -> en_gb_xx, en_gb, en, FALLBACK (if defined) or ""
              but really what if there was en_gb, en_us, and fr
             how would en_xx fall back to en_gb/en_us ? unless we knew about it 
- Simplification/reogainzation 2
  - 'hard coded' for string, numbers, errors, custom
    - based on FluentStirng, FluentNumber
    - custom -> toString
    - these would have custom toString and select() methods.
      - e.g., numbers -> NUMBER
      - string -> STRING
      - temporal -> default temoporal
      - the FluentXXX do TWO things
        - 1) keeps the formatting when formatted
        - 2) keeps the original object (unformatted) to be used as needed if so
    - reducer: JOIN (or rename to LIST?)
      - cannot be changed (?)
      - implicit
      - need only be specified when list format parametters need to be specified
      - runs formatter or select for each item
    - custom functions could implement toString and (optionally) select
  - toFluentValue(Any) toFluentCollection(Set) toFluentCollection(List)
    - toFluentValue(Any) -> toFluentCollection if Any implements Collection
    - use List.of(a)    because very fast for single lists
    - FluentCustom -> record
    - FluentValues:
      - ALL should retain original object.
      - FluentCustom treated as string UNLESS overridden with implicit
      - format(Scope) and select() NOT PART OF FLUENTVALUE
            FluentValues should have NO KNOWLEDGE of implicits. that is handled by the bundle
      - basically wrapper types, and an error   
      - ELIMINATE defaultFluentValueFactory; use methods in interface 'fluentvalue'
        - NO NEED to add more types for custom. custom is just a holder.
        - SEAL the type hierarchy, and ONLY use records.
  - implicits should be hardcoded. do not allow override.
    - but when we do, use methodHandle
  - HOWEVER
      - use setImplicit(class, "functionName")
        - should allow arguments (default arguments), can be overriden if actual function used
        - arguments should be processed during construction          
      - or use setImplicit(class<x>, lambda x -> String)
          - these would allow formatting automatically to a function or custom lambda
          - (and maybe use varhandle)
          - use LinkedHashMap and iterate:
              - exact match of class preferred, otherwise instnaceof type match (in class hierarchy)
          - HashMap for exact matching (.class -> lambda or function)
          - Array for linear scan for instanceof (array of tuple objects)
          - use simple builder so both can be made immutable
  - SELECT and LISTS (collections)
    - { NUMBER($score, minimumFractionDigits: 1) -> 
    -     [0.0] no points! too bad!
    -   *[other] you scored { NUMBER{$score, minimumFraactionDigits: 5) } points
    - For the above: select occurs for EACH ITEM processed as NUMBER() above
    - the current item is what is referred to in the selection arm (NOT the whole list)
  - option 'select' for string/number formatters to change what is selected on
    - (cardinal, ordinal, exact, something elese)
  - JOIN() for lists
      - 
- add DATE, TIME, DATETIME back
- semantic skeletons CLDR
  - https://www.unicode.org/reports/tr35/tr35-73/tr35-dates.html#Semantic_Skeletons
- API refinements
     - simplicity for general use case
     - AST: 
       - sealed interfaces + records, null OK (but noted)
       - then we can use pattern matching, switches
    - flexibility for more advanced uses cases
    - manage fallback Locale(s) ? 
    - easier parsing of FTL from a resource / URI / file
      - at least for the general case
- Plural selection and Functions
  - Locale-dependent functions (plural selection) should be cached
  - other functions (most!) which are idempotent can be initialized once and reused
  - review 'select' and options (cardinal, ordinal, etc.)
- Test Framework
    - more tests!
    - more detailed parser-level tests
    - assure spec compliance
    - more detailed function tests
    - better test organization
    - JSON serializer for checking AST?
- Message rendering:
    - e.g., StringBuilder initial size; look at best defaults for general case
    - perhaps allow parameter tuning in FluentBundle.Builder ?
- Performance
    - parsing (some work has been done on this)
    - make sure we use JDK intrinsic where applicable
      - Arrays.xxx
      - string.indexOf
      - https://chriswhocodes.com/hotspot_intrinsics_openjdk23.html
    - vectorization? SWAR?
    - formatting/rendering
        - need to profile
        - JMH harness
        - consider caching, etpattc. but caution w.r.t. concurrency
- FunctionResources
    - consider implemented threadsafe (and weak?) cache that
        can be used by function implementations; this would depend on
        performance feedback
  
    

Architectural Considerations
----------------------------
- change Resolver to use a switch instead of calling resolve() on AST nodes. This would more cleanly separate logic from AST and allow similar resolution logic to exist in a single
  file 
- Re-evaluate (particularly for JDK17) FTLStream and basic parsing/character conversion
- Vectorization? SWAR?
- AST: more use of record classes? but watch w.r.t. accessors and nullability
- 'current item' for use in selectors. For example : when selecting over a list, allow 
  a reference to the currently item, not just the entire List. Something like '$_'. 
  However, '$_' is not a legal identifier per spec.  

Future Directions
-----------------
- AST optimization (likely as an optional step)
    - quoted text merging, escape sequences merging
    - merge adjacent TextElements into a single TextElement
- AST compilation to bytecode per-Locale?
- use annotations, to make an enum of resource keys, with strong typing: https://l10nmessages.io/docs/fluent-api/
