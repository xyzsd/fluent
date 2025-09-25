/*
 *
 *  Copyright (c) 2025, xyzsd (Zach Del)
 *
 *  Licensed under either of:
 *
 *    Apache License, Version 2.0
 *       (see LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0)
 *    MIT license
 *       (see LICENSE-MIT) or http://opensource.org/licenses/MIT)
 *
 *  at your option.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *
 *
 */

package test;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.function.functions.list.CountFn;
import fluent.syntax.parser.FTLParser;
import fluent.syntax.parser.FTLStream;
import fluent.syntax.parser.ParseException;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// High-level tests of functionality
///
/// NOTE: Watch leading alignment in multi-line text blocks!
class FTLParserSmokeTest {

    static final boolean SHOW_TEST_RESULTS = true;

    static final Consumer<FluentBundle.ErrorContext> ERROR_LOGGER = (ec) -> {
        System.err.printf( "ERROR encountered during formatting of message '%s' (%s), locale %s; [%d errors]:\n",
                ec.messageID(), ec.attributeID(), ec.locale(), ec.exceptions().size() );
        ec.exceptions().forEach( e -> System.err.println( "  " + e ) );
    };

    private static FluentBundle parse(String in) {
        final FluentResource parse = FTLParser.parse( FTLStream.of( in ) );
        if (parse.hasErrors()) {
            System.err.println( "errors on parse: " + parse.errors() );
        }

        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .addFactory( CountFn.COUNT )
                .build();

        return FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( parse )
                .withLogger( ERROR_LOGGER )
                .build();
    }

    private static String msg(String in, String id) {
        return msg( in, id, Map.of() );
    }

    private static String msg(String in, String id, Map<String, Object> args) {
        assertNotNull( id );
        assertNotNull( args );

        FluentBundle bundle = parse( in );
        final String formatted = bundle.format( id, args );
        if (SHOW_TEST_RESULTS) {
            System.out.println( "msg() '" + formatted + "'" );
        }
        return formatted;
    }

    private static String msg(String in, String msgID, String attribID) {
        assertNotNull( msgID );
        assertNotNull( attribID );

        FluentBundle bundle = parse( in );
        final String formatted = bundle.format( msgID, attribID, Map.of() );
        if (SHOW_TEST_RESULTS) {
            System.out.println( "msg() '" + formatted + "'" );
        }
        return formatted;
    }

    private static void printMsg(String result, List<Exception> errors) {
        if (SHOW_TEST_RESULTS) {
            if (errors.isEmpty()) {
                System.out.println( "msg() '" + result + "' (no errors)" );
            } else {
                System.out.printf( "msg() '%s' %d error(s): %s\n",
                        result,
                        errors.size(),
                        errors
                );
            }
        }
    }

    @Test
    void helloTest() {

        assertEquals(
                "Hello, world!",
                msg( "hello = Hello, world!\n", "hello" )
        );

        assertEquals(
                "Hello, world!",
                msg( "hello = Hello, world!\r\n", "hello" )
        );
    }

    @Test
    void highLevelTestNoArgs() {
        FluentBundle bundle = parse( "hello = Hello, world!\n" );
        String output = bundle.format( "hello" );
        assertEquals(
                "Hello, world!",
                output

        );
        if (SHOW_TEST_RESULTS) {
            System.out.printf( "highLevel: '%s'\n", output );
        }
    }

    @Test
    void highLevelTestSimple() {
        FluentBundle bundle = parse( "hellothere = Hello there, {$name}!\n" );
        String output = bundle.format( "hellothere", Map.of( "name", "Fluent User" ) );

        assertEquals(
                "Hello there, Fluent User!",

                output
        );
        if (SHOW_TEST_RESULTS) {
            System.out.printf( "highLevel: '%s'\n", output );
        }
    }

    @Test
    void helloEOFTest() {
        // no line terminator
        assertEquals(
                "Hello, wo",
                msg( "hello = Hello, wo", "hello" )
        );
    }

    @Test
    void quoteTest() {
        assertEquals(
                "A brace { lives here.",
                msg( "quote = A brace {\"{\"} lives here.", "quote" )
        );

        assertEquals(
                "A \\.",
                msg( "quote = A {\"\\\\\"}.", "quote" )
        );

        assertEquals(
                "A \".",
                msg( "quote = A {\"\\\"\"}.", "quote" )
        );
    }

    @Test
    void quoteUnicodeTest() {
        assertEquals(
                "A \uD83D\uDE02.",
                msg( "quote = A {\"\\U01F602\"}.", "quote" )
        );

        assertEquals(
                "A \u2014.",
                msg( "quote = A {\"\\u2014\"}.", "quote" )
        );
    }

    @Test
    void blankUnicodeTest() {
        // These tests use string concatenation because triple-quotes strip trailing whitespace
        String thinSpace = String.valueOf( Character.toChars( 0x2009 ) );
        System.out.println("  thinSpace = "+Character.toChars( 0x2009 ).length);
        assertEquals(
                thinSpace,
                msg( "quote =" + thinSpace, "quote" )
        );

        assertEquals(
                thinSpace,
                msg( "-t =" + thinSpace + "\nquote = { -t }", "quote" )
        );

        String s1 = "example = { NUMBER($cnt) ->\n    [one] hello\n    *[other]" + thinSpace + "\n}";

        assertEquals(
                thinSpace,
                msg( s1, "example", Map.of( "cnt", 0 ) )
        );
    }

    @Test
    void literalTest() {
        String s1 = """
                literal-string = {"abc123"}
                literal-number-test1 = {123}
                literal-number-test2 = {-3.14159265358}
                """;

        assertEquals(
                "abc123",
                msg( s1, "literal-string" )
        );

        assertEquals(
                "123",
                msg( s1, "literal-number-test1" )
        );

        // default number formatting is up to 6 (!) decimal places.
        assertEquals(
                "-3.141593",
                msg( s1, "literal-number-test2" )
        );
    }

    @Test
    void termTest1() {
        String s1 = """
                -brand-name = SuperAwesomeProduct
                update-successful = { -brand-name } has been updated.
                """;

        assertEquals(
                "SuperAwesomeProduct has been updated.",
                msg( s1, "update-successful" )
        );
    }

    @Test
    void termTest2() {
        String s1 = """
                -brand-name = SuperAwesomeProduct\r
                update-successful = { -brand-name } has been updated.\r
                """;
        assertEquals(
                "SuperAwesomeProduct has been updated.",
                msg( s1, "update-successful" )
        );

    }

    @Test
    void parameterizedTermTest() {
        // contrived example as per docs, but simple; avoids selectors (separate test)
        String s1 =
                """
                        -https = https://{ $host }
                        visit = Visit { -https(othervalue: "asdf", host: "example.com") } for more information.
                        """;

        assertEquals(
                "Visit https://example.com for more information.",
                msg( s1, "visit" )
        );


        // contrived example as per docs, but simple; avoids selectors (separate test)
        s1 =
                """
                        -https = https://{ $host }
                        visit = Visit { -https(host: "example.com") } for more information.
                        """;

        assertEquals(
                "Visit https://example.com for more information.",
                msg( s1, "visit" )
        );


        // positionals arguments in term references will cause an exception during parse
        s1 = """
                -https = https://{ $host }
                visit = Visit { -https("positional1", "positional2", host: "example.com") } for more information.
                """;

        final FluentResource parse = FTLParser.parse( FTLStream.of( s1 ) );
        assertEquals( parse.errors().size(), 1 );
        assertEquals( parse.errors().get( 0 ).errorCode(), ParseException.ErrorCode.E0031 );
    }

    @Test
    void missingTermTest() {
        String s1 = """
                -new-brand-name-old = SuperAwesomeProduct
                update-successful = { -brand-name } has been updated.
                """;

        // one ReferenceException will be generated. This test is correct though
        // we don't yet verify the exception
        assertEquals(
                "{Unknown term: -brand-name} has been updated.",
                msg( s1, "update-successful" )
        );


    }

    @Test
    void messageRefTest() {
        // test of a message referring to another message
        String s = """
                option-name = Permanently Destroy
                option-message = NEVER click "{ option-name }!"
                """;
        assertEquals(
                "NEVER click \"Permanently Destroy!\"",
                msg( s, "option-message" )
        );
    }

    @Test
    void messageWithAttributes() {
        String s = """
                contact = Contact Information
                    .phone = 111-222-3333
                    .email = user@host.com
                another-message = another
                """;

        assertEquals(
                "Contact Information",
                msg( s, "contact" )
        );

        assertEquals(
                "111-222-3333",
                msg( s, "contact", "phone" )
        );

        assertEquals(
                "user@host.com",
                msg( s, "contact", "email" )
        );


        s = """
                contact = Contact Information\r
                    .phone = 111-222-3333\r
                    .email = user@host.com\r
                another-message = another\r
                """;

        assertEquals(
                "Contact Information",
                msg( s, "contact" )
        );

        assertEquals(
                "111-222-3333",
                msg( s, "contact", "phone" )
        );

        assertEquals(
                "user@host.com",
                msg( s, "contact", "email" )
        );
    }

    @Test
    void messageWithAttributes2() {
        // deliberately empty first line ('contact' has no text after the '=')
        // this is legal as long as one or more attributes are present
        String s = """
                contact = 
                    .phone = 111-222-3333
                    .email = user@host.com
                another-message = another
                """;

        assertEquals(
                "111-222-3333",
                msg( s, "contact", "phone" )
        );

        assertEquals(
                "user@host.com",
                msg( s, "contact", "email" )
        );

        assertEquals(
                "{No pattern specified for message: 'contact'}",
                msg( s, "contact" )
        );
    }

    @Test
    void messageAttributeRefTest() {
        //  test message referring to ATTRIBUTES in another message
        String s = """
                message = a general message
                    .text = a TEXT message
                    .phone = a TELEPHONE message
                
                output = { message } has been sent.
                output2 = { message.text } has been sent.
                output3 = {message.phone} has been sent.
                invalid = {message.badattribute} has been sent.
                """;

        assertEquals(
                "a TEXT message has been sent.",
                msg( s, "output2" )
        );

        assertEquals(
                "a general message has been sent.",
                msg( s, "output" )
        );


        // no pattern (pattern) after message
        s = """
                message =
                    .text = a TEXT message
                    .phone = a TELEPHONE message
                
                output = { message } has been sent.
                output2 = { message.text } has been sent.
                output3 = {message.phone} has been sent.
                """;

        assertEquals(
                "a TEXT message has been sent.",
                msg( s, "output2" )
        );

        // an error, because 'message' alone is invalid
        assertEquals(
                "{No pattern specified for message: 'message'} has been sent.",
                msg( s, "output" )
        );
    }

    @Test
    void multiLineTest() {
        // no text blocks here for clarity
        // we are using both \n and \n\r line terminators to make sure both work
        String ml = "multi =  \n" +
                " First.\n" +
                " Second.\n";

        assertEquals(
                "First.\nSecond.",
                msg( ml, "multi" )
        );

        ml = "multi =  \r\n" +
                " First.\r\n" +
                " Second.\r\n";

        assertEquals(
                "First.\nSecond.",
                msg( ml, "multi" )
        );

        ml = "multi =  \n" +
                " First.\n" +
                " \n" +      // blank!
                " Second.\n";

        assertEquals(
                "First.\n\nSecond.",
                msg( ml, "multi" )
        );

        ml = "multi =  \r\n" +
                " First.\r\n" +
                " \r\n" +      // blank!
                " Second.\r\n";

        assertEquals(
                "First.\n\nSecond.",
                msg( ml, "multi" )
        );

        // 'Second' is NOT indented; it is a continuation
        ml = "multi =  First.\n" +
                "   Second.\n";

        assertEquals(
                "First.\nSecond.",
                msg( ml, "multi" )
        );

        // 'Second' is NOT indented. It is a continuation
        ml = "multi =  First.\r\n" +
                "   Second.\r\n";

        assertEquals(
                "First.\nSecond.",
                msg( ml, "multi" )
        );
    }

    @Test
    void spaceyTest() {
        String s = "leading-spaces =     This message's pattern starts with the word \"This\".\n";
        assertEquals(
                "This message's pattern starts with the word \"This\".",
                msg( s, "leading-spaces" )
        );
    }

    @Test
    void blankeyTest() {
        // NOTE: be mindful of space AFTER a newline (there shouldn't be any);
        //       spaces after newlines will count as prepended spaces on the next line.
        //       While this makes sense, it's not always obvious.
        //       Save your future self debugging time by remembering this!

        String s =
                "leading-lines =\n" +
                        "\n" +
                        "\n" +
                        "    This message's pattern also starts with the word \"This\".\n";
        assertEquals(
                "This message's pattern also starts with the word \"This\".",
                msg( s, "leading-lines" )
        );

        s = "leading-lines =\r\n" +
                "\r\n" +
                "\r\n" +
                "    This message's pattern also starts with the word \"This\".\r\n";
        assertEquals(
                "This message's pattern also starts with the word \"This\".",
                msg( s, "leading-lines" )
        );

        s = "blank-lines =\n" +
                "\n" +
                "    Blank above ignored.\n" +
                "\n" +
                "    Blank above preserved.";
        assertEquals(
                "Blank above ignored.\n\nBlank above preserved.",
                msg( s, "blank-lines" )
        );

        s = "blank-lines =\r\n" +
                "\r\n" +
                "    Blank above ignored.\r\n" +
                "\r\n" +
                "    Blank above preserved.";
        assertEquals(
                "Blank above ignored.\n\nBlank above preserved.",
                msg( s, "blank-lines" )
        );
    }

    @Test
    void multiLineIndentTest() {
        String s = "indent =\n" +
                "   Common indent\n" +
                "     2sp indent\n" +
                "        5sp indent\n" +
                "   Common indent is 3sp\n";

        assertEquals(
                "Common indent\n  2sp indent\n     5sp indent\nCommon indent is 3sp",
                msg( s, "indent" )
        );

        s = "indent = No indent this line.\n" +
                "     2sp indent\n" +
                "   No indent last line.\n";

        assertEquals(
                "No indent this line.\n  2sp indent\nNo indent last line.",
                msg( s, "indent" )
        );

        s = "indent =       Still no indent this line.\n" +
                "     2sp indent\n" +
                "   No indent last line.\n";

        assertEquals(
                "Still no indent this line.\n  2sp indent\nNo indent last line.",
                msg( s, "indent" )
        );
    }

    @Test
    void simpleVariableTest() {
        // pattern as a FluentString
        assertEquals(
                "Hello, world!",
                msg( "hello = Hello, {$name}!\n", "hello", Map.of( "name", new FluentString( "world" ) ) )
        );

        // pattern as a String
        assertEquals(
                "Hello, world!",
                msg( "hello = Hello, {$name}!\n", "hello", Map.of( "name", "world" ) )
        );
    }

    @Test
    void simpleFunctionTestForCOUNT() {
        // COUNT() / no args
        assertEquals(
                "Hello, 0!",
                msg( "hello = Hello, {COUNT()}!\n", "hello", Map.of( "name", "world" ) )
        );

        // COUNT() 1 arg with 1 pattern
        assertEquals(
                "Hello, 1!",
                msg( "hello = Hello, {COUNT($name)}!\n", "hello", Map.of( "name", new FluentString( "world" ) ) )
        );

        // COUNT() 2 args with 1 pattern each
        assertEquals(
                "Hello, 2!",
                msg( "hello = Hello, {COUNT($name, $age)}!\n", "hello",
                        Map.of( "name", new FluentString( "world" ),
                                "age", FluentNumber.of( 99 ) ) )
        );

        // COUNT() 1 arg with a list of 3 items
        assertEquals(
                "Hello, 3!",
                msg( "hello = Hello, {COUNT($names)}!\n", "hello",
                        Map.of( "names", List.of( "Alfonso", "Betty", "Charlie" ),
                                "age", FluentNumber.of( 99 ) ) )
        );

        // COUNT() 2 args with a list and a non-list
        assertEquals(
                "Hello, 4!",
                msg( "hello = Hello, {COUNT($names, $age)}!\n", "hello",
                        Map.of( "names", List.of( "Alfonso", "Betty", "Charlie" ),
                                "age", FluentNumber.of( 99 ) ) )
        );
    }

    @Test
    void simpleLISTTest() {
        // one item
        assertEquals(
                "Hello, world!",
                msg( "hello = Hello, {$names}!\n", "hello", Map.of( "names", "world" ) )
        );

        assertEquals(
                "Hello, world!",
                msg( "hello = Hello, {$names}!\n", "hello", Map.of( "names", new FluentString( "world" ) ) )
        );

        // implicit
        assertEquals(
                "Hello, Alfonso, Betty!",
                msg( "hello = Hello, {$names}!\n", "hello",
                        Map.of( "names", List.of( "Alfonso", "Betty" ) ) )
        );

        // explicit
        assertEquals(
                "Hello, Alfonso and Betty!",
                msg( "hello = Hello, {LIST($names, type:\"and\", width:\"wide\")}!\n", "hello",
                        Map.of( "names", List.of( "Alfonso", "Betty" ) ) )
        );

        // implicit 3 default
        assertEquals(
                "Hello, Alfonso, Betty, Charlie!",
                msg( "hello = Hello, {$names}!\n", "hello",
                        Map.of( "names", List.of( "Alfonso", "Betty", "Charlie" ) ) )
        );

        // explicit 3 default
        assertEquals(
                "Hello, Alfonso, Betty, Charlie!",
                msg( "hello = Hello, {LIST($names)}!\n", "hello",
                        Map.of( "names", List.of( "Alfonso", "Betty", "Charlie" ) ) )
        );

        // explicit 3 using 'or'
        assertEquals(
                "Hello, Alfonso, Betty, or Charlie!",
                msg( "hello = Hello, {LIST($names, type:\"or\", width:\"wide\")}!\n", "hello",
                        Map.of( "names", List.of( "Alfonso", "Betty", "Charlie" ) ) )
        );
    }

    @Test
    void missingVariableTest() {
        assertEquals(
                "Hello, {Unknown variable: $name}!",
                msg( "hello = Hello, {$name}!\n", "hello", Map.of( "xxxxx", "world" ) )
        );
    }

    /// https://www.unicode.org/cldr/charts/45/supplemental/language_plural_rules.html
    ///  COUNT() -> number; number default select is cardinal plural form.
    /// for US English: those are 'one' and 'other'
    @Test
    void selectorWithFunctionChain3() {
        String s1 = """
                owned = You own { COUNT($names) ->
                     [one] a thing
                    *[other] many things
                 }.
                """;
        assertEquals(
                "You own many things.",
                msg( s1, "owned", Map.of( "names", List.of( "car", "boat", "plane" ) ) )
        );
    }

    @Test
    void selectorWithFunctionChain1Exact() {
        String s1 = """
                owned = You own { NUMBER(COUNT($names), kind:"exact") ->
                     [1] ONE thing
                     [2] TWO things
                     [one] a thing
                    *[other] many things
                 }.
                """;
        assertEquals(
                "You own ONE thing.",
                msg( s1, "owned", Map.of( "names", List.of( "a car" ) ) )
        );
    }

    @Test
    void selectorWithFunctionChain2Exact() {
        String s1 = """
                owned = You own { NUMBER(COUNT($names), kind:"exact") ->
                     [1] ONE thing
                     [2] TWO things
                     [one] a thing
                    *[other] many things
                 }.
                """;
        assertEquals(
                "You own TWO things.",
                msg( s1, "owned", Map.of( "names", List.of( "a car", "a boat" ) ) )
        );
    }

    @Test
    void selectorWithFunctionChain1() {
        String s1 = """
                owned = You own { COUNT($names) ->
                     [1] ONE thing
                     [2] TWO things
                     [one] a thing
                    *[other] many things
                 }.
                """;
        assertEquals(
                "You own a thing.",
                msg( s1, "owned", Map.of( "names", List.of( "just a car" ) ) )
        );
    }

    @Test
    void selectorTermTest() {
        String s1 = """
                -thing = { $count ->
                    [one] thing
                   *[other] things
                 }
                
                you-own = You have { $count ->
                     [one] a {-thing(count: "one")}
                    *[other] {-thing(count: "other")}
                 }.
                
                """;

        assertEquals(
                "You have a thing.",
                msg( s1, "you-own", Map.of( "count", "one" ) )
        );

        assertEquals(
                "You have things.",
                msg( s1, "you-own", Map.of( "count", "ten" ) )
        );


    }

}
