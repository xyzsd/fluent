# fluent
A Java implementation of the [Mozilla Project Fluent][mozFluentGH] localization 
framework. The Fluent framework is designed to unleash the expressive power of
natural language translations.

The syntax of the Fluent Translation List, `FTL` is designed to be simple, yet is powerful 
enough to represent complex natural-language constructs such as plurals, conjugations,
and gender. Learn more about Project Fluent at [projectfluent.org][mozProjectFluent].

Status
------
The 2.0 release is now available!

The 2.0 version has marked improvements in the code base, with an improved API, test coverage, and examples.  
A single dependency remains (ICU). All FTL parser specification tests pass, including handling of LF and CRLF line 
endings (and intermixed variants), early terminations, and general aberrant and hostile test cases. 
New, and better-tested built-in functions are included. It also much easier to create, add, or rename existing functions. 

The 2.0 version targets JDK 23, though without too much work JDK 21 could be an additional target.

   

Introductory Example
--------------------
Given the following example FTL:
```
# Simple things are simple.
hello-user = Hello, {$userName}!

# Complex things are possible.
shared-photos =
    {$userName} {$photoCount ->
        [one] added a new photo
       *[other] added {$photoCount} new photos
    } to {$userGender ->
        [male] his stream
        [female] her stream
       *[other] their stream
    }.
```
We can use it as follows:
```java
// Setup the function registry. This is the simplest way to set it up, but will only include
// the required built-in functions. The function registry can be shared by different bundles / different locales.
final FluentFunctionRegistry registry = FluentFunctionRegistry.builder().build();

// Read the FTL (which is localized) and parse it into the data model (AST).
FluentResource resource = FTLParser.parse(  Thread.currentThread().getContextClassLoader(), "hello.ftl" );
if (!resource.errors().isEmpty()) {
        // The FluentResource also contains errors encountered during parsing.
        System.err.printf("Encountered %d errors during parsing!\n", resource.errors().size());
        resource.errors().forEach(System.err::println);
}

// Create the FluentBundle, which is Locale-dependent. 
// The FluentBundle is what we use to manipulate the data model and render localized messages.
final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
        .addResource( resource )
        .build();

// Now let's render some messages!
// Variables in the FTL message are substituted using name-value pairs stored in a Map.
// A Map<String, ?> provides the parameters to substitute.
//
// This is the simplest way to format a localized message.
// Say hello to the user, "Billy".
final String helloUser = bundle.format(
        "hello-user",               // the message key, as defined in the FTL file
        Map.of("userName", "Billy") // our single item map
);
System.out.println( helloUser );        // output: "Hello, Billy!"


// Now, let's try a more complex message, which uses a selector and 3 variables.
// output: "Billy added a new photo to his stream."
final String sharedPhotoMessage = bundle.format(
        "shared-photos",    // the message key, defined in the FTL file
        Map.of(
                "userName", "Billy",      // userName, as above
                "photoCount", 1,          // photoCount
                "userGender", "male"
        )
);
System.out.println( sharedPhotoMessage );

// Another example using the same message, but with different parameters.
// output: "Chris added 10 new photos to their stream."
Map<String, ?> arguments = Map.of(
        "userName", "Chris",
        "userGender", "unspecified",
        "photoCount", 10
);
System.out.println( bundle.format("shared-photos", arguments) );
```

### Support for parameters containing lists 
There is built-in support for `SequencedCollection` (which includes `List` and `SortedSet` among others, but for
purposes of discussion we will refer to any such collection as a list). The `SequencedCollection` type is 
required to ensure output stability.

Note that selection over lists is not supported. This can be worked around; see the `list-selection` example for details.

Lists can be heterogeneous (e.g., mix of numbers and strings), but not all functions can handle heterogeneous lists.
Refer to the fluent function documentation for details.

From the FTL used above, here is a simple example:

```
// example output here assumes Locale.US 
// 3-item list
System.out.println(
        bundle.format(
        "hello-user",
        Map.of("userName", List.of("Billy", "Willy", "Lilly"))
    )
);
// output: "Hello, Billy, Willy, Lilly!" 

// 2-item list
System.out.println(
        bundle.format(
        "hello-user",
        Map.of("userName", List.of("Willy", "Lilly"))
    )
);
// output: "Hello, Willy, Lilly!" 
```

The list output can be customized using CLDR-localized list formatting. For `Locale.US`, items are separated
with commas by default (for any list size). However, list formatting can be customized using the built-in LIST 
function.

Given this FTL:
```
hello-all-users = Hello, { LIST($users, type:"and", width:"wide") }!
```
We now get the following output:
```
// example output here assumes Locale.US 
// 3-item list
System.out.println(
        bundle.format(
        "hello-all-users",
        Map.of("userName", List.of("Billy", "Willy", "Lilly"))
    )
);
// output: "Hello, Billy, Willy, and Lilly!" 

// 2-item list
System.out.println(
        bundle.format(
        "hello-all-users",
        Map.of("userName", List.of("Willy", "Lilly"))
    )
);
// output: "Hello, Willy and Lilly!" 
```
Similarly, for the following FTL, but using bundle localization `Locale.FRANCE`:
```
hello-all-users = Bonjour, { LIST($users, type:"and", width:"wide") }!
```
Output: (using above code). Note that the conjunction is automatically localized to 'et'. 
```
Bonjour, Billy, Willy et Lilly! 
Bonjour, Willy et Lilly!
```

### Supported Types
During parameter substitution, the following types are supported:
   * Strings
   * Numeric Types:
      * `long` (with narrower types treated as a `long`)
      * `double` (and narrower floating types)
      * `BigDecimal` (and `BigInteger`)
         * useful to retain precision, particularly trailing zeros
   * `TemporalAccessor` implementations (e.g., `LocalDateTime`, etc.)

Custom types can be added as needed. For a simple example, refer to `BooleanFn` and associated tests.

Differences from *fluent.js*
--------------------------------------------
### DATETIME()
DATETIME as implemented here does not try to re-implement JavaScript's Intl.DateTime. For more precise formatting,
use the TEMPORAL() function insetead, which supports pattern-based (semantic skeleton) formatting in addition 
to predefined forms.

### Functions
A number of additional functions are included. More functions can be easily added, and existing functions can
removed or changed.

Baseline (implicit) functions:
- NUMBER()
  - handles localization of numeric values and pluralization (cardinal and ordinal forms). Provides formatting
    using custom patterns / semantic skeletons.
- LIST()
  - handles custom formatting of lists
- DATETIME()
  - simple formatting of Date and Time values.

Additional functions:
  - COUNT()
    - counts the number of arguments supplied to the function
  - NUMSORT()
    - sorts numerical arguments
  - STRINGSORT()
    - sorts string arguments
  - BOOLEAN()
    - formats Boolean values to strings ('true' or 'false') or numbers ('0' or '1')
  - ABS()
    - absolute value
  - OFFSET()
    - offset all integral values by a specific amount
  - SIGN()
    - determine the sign of a numeric value, as a string. For decimal values, also 
      handles NaN and infinities.
  - CASE()
    - case conversion
  - TEMPORAL()
    - `TemporalAccessor` formatting using predefined patterns or custom patterns (semantic skeletons)
  - XTEMPORAL()
    - extract a field from a `TemporalAccessor` (for example, the hour field from `LocalDateTime`, as a numeric value).


Function Composition
--------------------
Functions can be composed. For example, given the following FTL:

```
example = { NUMBER(NUMSORT($list, order:"descending"), minimumFractionDigits:2, useGrouping:"true") }
```

and associated code:

```java
// ... assumes FluentResourceBundle 'bundle' already created ...

final List<Number> NUMLIST = List.of(
        3184, 538754, 1734.3489, 193547.37771, 0L, 0.0d, 
        new BigDecimal( "193547.37772" ), 
        new BigDecimal( "-10.000001000" ), 
        new BigDecimal( ".00000120" ),
        Double.POSITIVE_INFINITY, 
        Double.NEGATIVE_INFINITY
        );

String result = bundle.format( "example", Map.of( "$list", NUMLIST ) );
System.out.println(result);
```

`result` will be `∞, 538,754.00, 193,547.37772, 193,547.37771, 3,184.00, 1,734.3489, 0.0000012, 0.00, 0.00, -10.000001, -∞`.


### Dependencies
Fluent depends on *[ICU][icuPlurals]* for language pluralization rules and also (currently) number and list formatting.


Documentation
-------------
Available for [download][dlMavenCentral_base] or [online][docsOnlineBase].

Download
--------
[Download][dlJAR] the latest JAR or depend via Maven:

```xml
<dependency>
   <groupId>net.xyzsd.fluent</groupId>
   <artifactId>fluent-base</artifactId>
   <version>2.0</version>
   <type>module</type>
</dependency>
```

or Gradle:
```kotlin
implementation("net.xyzsd.fluent:fluent-base:2.0")
```

### Working with `-SNAPSHOT` Versions
Snapshot versions may be available from the Central Repository. 

The specific snapshot must be specifically requested. Please note that -SNAPSHOT releases
are for development only, may not be stable, and will be automatically removed 90 days after
creation.

To use a snapshot, setup your `build.gradle.kts` file as so:
```kotlin
repositories {
    maven {
        setUrl("https://central.sonatype.com/repository/maven-snapshots/")
        name = "Central Portal Snapshots"

        // Only search this repository for the specific dependency
        content {
            includeModule("net.xyzsd.fluent", "fluent-base")
        }
    }

    mavenCentral()
}
```
and then in the dependencies section specify the snapshot:
```kotlin
dependencies {
    implementation("net.xyzsd.fluent:fluent-base:2.0-SNAPSHOT")
    // ... etc.
    // ...
}
```

Acknowledgements
----------------
Portions of this project are based on `fluent-rs`.

License
-------
Copyright 2021, 2025, 2026 xyzsd

Licensed under either of

 * Apache License, Version 2.0
   (see LICENSE-APACHE or [apache.org](http://www.apache.org/licenses/LICENSE-2.0)
 * MIT license
   (see LICENSE-MIT) or [opensource.org](http://opensource.org/licenses/MIT)

at your option.

[cldrPlurals]: https://github.com/xyzsd/cldr-plural-rules
[icuPlurals]: https://github.com/unicode-org/icu/tree/main/icu4j
[mozFluentGH]: https://github.com/projectfluent/fluent/
[mozProjectFluent]:  https://projectfluent.org/
[dlJAR]: https://github.com/xyzsd/fluent/releases
[dlMavenCentral_base]: https://central.sonatype.com/artifact/net.xyzsd.fluent/fluent-base/versions
[docsOnlineBase]: https://javadoc.io/doc/net.xyzsd.fluent/fluent-base/latest/index.html
