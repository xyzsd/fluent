# Project Thoughts, Architectural Considerations, and Future Directions

Near-term TODOs
----------------
- API shape refinements
    - simplicity for general use case
    - flexibility for more advanced uses cases
    - fallback Locale(s)
- Test Framework
    - more tests!
    - more detailed parser-level tests
    - more detailed function tests
    - JSON serializer for checking AST?
- Global Options
    - basics in place within FluentBundle and Builder
    - need to enable in methods/Scope, and test
- Message rendering:
    - e.g., StringBuilder initial size; look at best defaults for general case
    - perhaps allow paramter tuning in FluentBundle.Builder 
    - ? logging failures (SLF4J)
- Modularization
    - true module support (will need to alter Gradle build files)
    - determine accessibility of module internals
- Performance
    - parsing (some work has been done on this)
    - formatting/rendering
        - need to profile
        - JMH harness
        - consider caching, etc. but caution w.r.t. concurrency
- FunctionResources
    - consider implemented threadsafe (and weak?) cache that
        can be used by function implementations; this would depend on
        performance feedback
      

Architectural Considerations
----------------------------
- depending on status of sealed classes / pattern matching, change Resolver to 
  using switch instead of calling resolve() on AST nodes. This would more cleanly  
  separate logic from AST and allow similar resolution logic to exist in a single
  file
- Re-evaluate (particularly for JDK17) FTLStream and basic parsing/character conversion
- AST: more use of record classes? but watch w.r.t. accessors and nullability
- 'current item' for use in selectors. For example : when selecting over a list, allow 
  a reference to the currently item, not just the entire List. Something like '$_'. 
  However, '$_' is not a legal identifier per spec.  

Future Directions
-----------------
- AST optimization (likely as an optional step)
    - quoted text merging, escape sequences merging
    - merge adjacent TextElements into a single TextElement
- AST compilation to bytecode
    - would be per-Locale
    - look at JTE (Java template engine) / Manifold projects
