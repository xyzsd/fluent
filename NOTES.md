
# Notes, TODOs, and Roadmap

## Parser Notes
* Scalar/SIMD/SWAR
  * SIMD was eliminated. Most runs are too short and invokation too slow.
  * Scalar and SWAR are competitive. SWAR has been kept for now, and is used 
  for operations with longer spans.  
  * Testing on more hardware is needed. Older CPUs may benefit more from SWAR.

## Dependencies
* ideally we would further minimize dependencies.
* Currently, we have one runtime dependency: ICU4J
    * for plural handling (though this could replaced with [CLDR-Plurals](https://github.com/xyzsd/cldr-plural-rules))
    * list formatting (this can be replaced with JDK Listformat API, available in Java 22)
    * number formatting (ICU number formatter has advantages over JDK formatter).
      * could implement an alternate non-ICU4J dependent formatter, as an option

## Roadmap 
* ?JDK 21 version
  * could consider a JDK21 compatible version as well. Not too much would have to be changed 
    if we exclude the Javadoc format.
  
* Parser Considerations
    * Consider eliminating FTLStream class, and only using static methods
       * all methods would require (byte[], int offset)

* Structure / Performance
  * fluent.bundle.resolver.Scope: lazily convert arguments; could be more
    performant

* Rendering Performance 
    * Measurements: first, we need good benchmarks (JMH) for message rendering
        * investigate caching/cache performance
        * concatenation performance
        * target common data types (String, Numbers, lists)
        * target common functions (plural selection, temporal/date/time)
    * optimize StringBuilder/HashMap initial sizing (if possible)
    * potentially consider, depending upon measurements: 
      * Usage of StringConcatFactory and LambdaMetafactory, vs. bytecode generation
      * Perhaps only for commonly used entries, privately cached per-bundle
    * Usage-site code generation (perhaps using classfile API)
      * create methods for message entries  
      * what about parameter typing? 
    * ? Convert FTL AST to bytecode (for a given resource) (AOT vs. JIT)
      * could then load as class file.
  
    
    
