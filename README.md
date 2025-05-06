# fluent
A Java implementation of the [Mozilla Project Fluent][mozFluentGH] localization 
framework. The Fluent framework is designed to unleash the expressive power of
natural language translations.

The syntax of the Fluent Translation List, `FTL` is designed to be simple, yet is powerful 
enough to represent complex natural-language constructs such as plurals, conjugations,
and gender. Learn more about Project Fluent at [projectfluent.org][mozProjectFluent].

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
// read the FTL and parse it into the data model
FluentResource resource = FTLParser.parse( FTLStream.of( Files.readString("hello.ftl") ) );

// create the FluentBundle, which is used to manipulate the data model and perform localization
FluentBundle bundle = FluentBundle.builder( Locale.US, ICUFunctionFactory.INSTANCE )
        .addResource( resource )
        .build();

// The format() method is the simplest way to format a message.
final String helloUser = bundle.format(
        "hello-user",                       // the message name 
        Map.of( "userName", "Billy" )       // Map of parameters to substitute
        );
System.out.println( helloUser );        // output: "Hello, Billy!"

// The following examples using the same message, but with different parameters.
 
// output: "Anne added a new photo to her stream."
Map<String, ?> args1 = Map.of(
        "userName", "Anne",
        "userGender", "female",
        "photoCount", 1
        );
System.out.println( bundle.format("shared-photos", args1) ); 

// output: "Billy added 5 new photos to his stream."
Map<String, ?> args2 = Map.of(
        "userName", "Billy",
        "userGender", "male",
        "photoCount", 5
        );
System.out.println( bundle.format("shared-photos", args2) ); 

// output: "Chris added 10 new photos to their stream."
Map<String, ?> args3 = Map.of(
        "userName", "Chris",
        "userGender", "unspecified",
        "photoCount", 10
        );
System.out.println( bundle.format("shared-photos", args3) ); 
        
```

Status
------
 - [x] currently targeting JDK 17
 - [x] Usable&mdash;though not optimal&mdash;API. 
   - overall API shape may change. 
   - the goal is to keep easy things easy, and difficult things possible.
 - [x] Modularization: currently, automatic modules are used.
   - goal is full modularization support
 - [x] Tests: mosts tests are currently high-level
      - more tests are needed
      - better test organization
 - [ ] Documentation
      - not all classes have the documentation they deserve (... such as FluentBundle)
- [ ] Examples
  - Single simple example included currently
   
   



Differences from *fluent-rs* and *fluent.js*
--------------------------------------------
### DATETIME()
There is no DATETIME() function. Instead, use TEMPORAL().
The TEMPORAL() function supports pattern-based formatting in addition to predefined localized forms.

### NUMBER()
Most options of the NUMBER() function are supported. To match a number in a select clause as a String, rather than 
as a plural form (*consider this carefully*), type="string" must be specified.

See the NUMBER() function documentation (NumberFn) for more information.

### Support for parameters containing lists
Initial support has been added for parameters contained in a `List<?>` or a `Set<?>`. 
Lists can be heterogeneous. Functions can be nested and will be applied to each item. By default, the resultant list
will be comma-separated. However, using the JOIN() function, conjunctions and alternative delimiters are
supported. To preserve ordering, use `List` instead of  `Set`, or sort with STRINGSORT() or NUMSORT().

Support for lists extends to select statements as well; the select will apply to each item in the list. 
Note that there is currently no notion of or way to capture the item currently being iterated on in a select clause. 
Therefore, the use of lists in clauses with multiple select statements can be tricky.

Using the example FTL in the introduction:
```java
final String helloUser = bundle.format(
        "hello-user",                       
            Map.of( "userName", List.of("Billy","Silly","Willy" ) )   
        );
System.out.println( helloUser );        // output: "Hello, Billy, Silly, Willy!"
```

Though when lists are expected, the formatting can be customized:

```
# modified FTL from introductory example
hello-user = Hello, { JOIN($userName, separator:", ", junction:", and ", pairSeparator:" and ") }!
```

Now:
```java
// output: "Hello, Billy, Silly, and Willy!
System.out.println(
        bundle.format(
            "hello-user",                       
            Map.of( "userName", List.of("Billy","Silly","Willy" ) )   
        )
    );

// output: "Hello, Betty and Yeti!
System.out.println(
        bundle.format(
            "hello-user",
            Map.of( "userName", List.of("Betty","Yeti" ) )
        )
    );
```

### Supported Types
During parameter substitution, the following types are supported:
   * Strings
   * Numeric Types:
      * `long` (with narrower types treated as a `long`)
      * `double` (and narrower floating types)
      * `BigDecimal` (and `BigInteger`)
         * useful to retain precision, particularly trailing zeros
   * TemporalAdjuster implementations

Custom types can be added as needed.

### Built-in functions
Fluent depends on *[cldr-plural-rules][cldrPlurals]* or *[ICU][icuPlurals]* for language pluralization rules.
Either `fluent-functions-cldr` or `fluent-functions-icu` must be used along with the `fluent-base` package.
Both can be used simultaneously, though not within the same FluentBundle.

A number of additional functions are included. More functions can be easily added, and existing functions can 
removed or changed. 

Functions currently include:
   - NUMBER()
      - handles localization of numeric values and pluralization (cardinal and ordinal forms).
      - `useGrouping`, `minimumIntegerDigits`, `minimumFractionDigits`, `maximumFractionDigits`, 
        `minimumSignificantDigits`, and `maximumSignificantDigits` are supported.
      - when converting a number to its plural form, formatting is ignored and the number is used
        in its original form. If precise control over leading/trailing digits is needed, use a BigDecimal.
      - `style` can be used to display currency or percentages. 
      - `type` used to specify pluralization
   - TEMPORAL()
      - Currently used instead of DATETIME(). Implementing DATETIME in a manner similar to `Intl.DateTimeFormat`
        is complex but could be considered in the future.
   - JOIN()
      - Used to format lists. See multi-item lists above.
   - COUNT()
      - Count the number of items in a list.
   - NUMSORT()
      - Sort a list of numbers ascending or descending.
   - STRINGSORT()
      - Sort Strings
   - ABS()
      - Absolute value of a number
   - IADD()
      - Add an integer (or long) to an integer (or long)
   - COMPACT()
      - format a number using the localized compact representation
        for example `COMPACT(10000)` would become `10K`
   - CURRENCY()
      - format a number using the localized currency representation
   - DECIMAL()
      - format a number using a number-format pattern
   - SIGN()
      - sign of a number; e.g., `SIGN(5)` becomes `"positive"`
   - CASE()
      - localized conversion of Strings to upper or lower case.


Function Composition
--------------------
Functions can be composed. For example, given the following FTL:

```
example = { NUMBER(NUMSORT($list, order:"descending"), minimumFractionDigits:2, useGrouping:"true") }
```

and associated code:

```java
...

final List<Number> NUMLIST = List.of(
        3184, 538754, 1734.3489, 193547.37771, 0L, 0.0d, 
        new BigDecimal( "193547.37772" ), 
        new BigDecimal( "-10.000001000" ), 
        new BigDecimal( ".00000120" )
        );

String result = bundle.format( "example", Map.of( "$list", NUMLIST ) );
System.out.println(result);
```

`result` will be `538,754.00, 193,547.378, 193,547.378, 3,184.00, 1,734.349, 0.00, 0.00, 0.00, -10.00`.

Documentation
-------------
Available for [download][dlMavenCentral], with aggregated documentation for all packages [available here][aggDocs].

Online:
- [fluent-base][docsOnlineBase]
- [fluent-functions-cldr][docsOnlineCLDR]
- [fluent-functions-icu][docsOnlineICU]

Download
--------
[Download][dlJAR] the latest JARs or depend via Maven:

```xml
<dependency>
   <groupId>net.xyzsd.fluent</groupId>
   <artifactId>fluent-base</artifactId>
   <version>0.70</version>
   <type>module</type>
</dependency>
```
```xml
<dependency>
    <groupId>net.xyzsd.fluent</groupId>
    <artifactId>fluent-functions-cldr</artifactId>
    <version>0.70</version>
</dependency>
```
```xml
<dependency>
    <groupId>net.xyzsd.fluent</groupId>
    <artifactId>fluent-functions-icu</artifactId>
    <version>0.70</version>
</dependency>

```
or Gradle:
```kotlin
implementation("net.xyzsd.fluent:fluent-base:0.70")
implementation("net.xyzsd.fluent:fluent-functions-cldr:0.70")
implementation("net.xyzsd.fluent:fluent-functions-icu:0.70")
```

Only one of the `fluent-functions-...` packages is required along with `fluent-base`.

### Working with `-SNAPSHOT` Versions
Snapshot versions may be available from Maven central repository. 

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
            includeModule("net.xyzsd.fluent", "fluent-functions-icu")
        }
    }

    mavenCentral()
}
```
and then in the dependencies section specify the snapshot:
```kotlin
dependencies {
    implementation("net.xyzsd.fluent:fluent-base:0.72-SNAPSHOT")
    // ... etc.
    // ...
}
```



Acknowledgements
----------------
Portions of this project are based on `fluent-rs`.

License
-------
Copyright 2021, 2025 xyzsd

Licensed under either of

 * Apache License, Version 2.0
   (see LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0)
 * MIT license
   (see LICENSE-MIT) or http://opensource.org/licenses/MIT)

at your option.

[cldrPlurals]: https://github.com/xyzsd/cldr-plural-rules
[icuPlurals]: https://github.com/unicode-org/icu/tree/main/icu4j
[mozFluentGH]: https://github.com/projectfluent/fluent/
[mozProjectFluent]:  https://projectfluent.org/
[dlJAR]: https://github.com/xyzsd/fluent/releases
[aggDocs]: https://github.com/xyzsd/fluent/releases/download/v0.70/fluent-0.70-aggregated-javadoc.zip
[dlMavenCentral]: https://repo1.maven.org/maven2/net/xyzsd/fluent/fluent-base/0.70/
[docsOnlineBase]: https://javadoc.io/doc/net.xyzsd.fluent/fluent-base/latest/index.html
[docsOnlineCLDR]: https://javadoc.io/doc/net.xyzsd.fluent/fluent-functions-cldr
[docsOnlineICU]: https://javadoc.io/doc/net.xyzsd.fluent/fluent-functions-icu
