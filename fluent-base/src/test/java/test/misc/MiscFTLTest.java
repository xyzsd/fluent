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

package test.misc;

import fluent.bundle.FluentResource;
import fluent.syntax.parser.FTLParser;
import fluent.syntax.parser.FTLStream;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

///  Miscellaneous FTL tests. Early-EOF tests for parser correctness, some of which are not included in FTL test fixtures.
///  We are just testing the parser here; we are not creating a bundle.
public class MiscFTLTest {

    /// parse the given string
    private static FluentResource parse(String in) {
        return FTLParser.parse( FTLStream.of( in ), false );
    }

    private static void show(FluentResource resource) {
        System.out.println("BEGIN");
        resource.errors().forEach( System.out::println );
        System.out.println("-----");
        resource.entries().forEach( System.out::println );
        System.out.println("-----");
        resource.junk().forEach( System.out::println );
        System.out.println("END\n");
    }


    @Test
    public void attributeEarlyEOF_noEquals() {
        // missing '=' at EOF
        final String in =   """
                            message = Message pattern.
                                    .attrib""";

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 0 ) );
    }

    @Test
    public void attributeEarlyEOF_noID1() {
        // fails after dot, which is EOF (but also is line 2)
        final String in =   """
                            message = Message pattern.
                                    .""";

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 0 ) );
    }

    @Test
    public void attributeEarlyEOF_noID2() {
        // also fails, but instead of EOF, gives line 2 (where dot is)
        final String in =   """
                            message = Message pattern.
                                    .
                                    """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 2 ) );
    }

    @Test
    public void attributeEarlyEOF_noAttribute() {
        // this is OK
        final String in =   """
                            message = Message pattern.
                                    """;

        final FluentResource resource = parse( in );
        assertEquals( 0, resource.errors().size() );
        assertEquals( 1, resource.entries().size() );
    }


    @Test
    public void selectOK() {
        // this is OK
        final String in =   """
                            unread = { $value ->
                                [one] You have one unread message.
                                *[other] You have { $value } unread messages.
                            }""";

        final FluentResource resource = parse( in );
        assertEquals( 0, resource.errors().size() );
        assertEquals( 1, resource.entries().size() );
    }

    @Test
    public void selectNoVariant() {
        // no variants!
        final String in =   """
                            unread = { $value ->
                                 
                            }""";

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0011, 3 ) );
    }

    @Test
    public void selectNoVariantEOF() {
        // no variants! but occurs at EOF
        final String in =   """
                            unread = { $value ->
                            """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0011, 0 ) );
    }

    @Test
    public void selectJustLeftBracket() {
        // expect a variant key (identifier or number)
        final String in =   """
                            unread = { $value ->
                                [
                            """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0013, 0 ) );
    }

    @Test
    public void selectIncompleteIdentifier() {
        // no closing square bracket
        final String in =   """
                            unread = { $value ->
                                [id
                            """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 0 ) );
    }

    @Test
    public void selectIncompleteIdentifier2() {
        // no closing square bracket, but for a number
        final String in =   """
                            unread = { $value ->
                                [34 some words
                            """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 2 ) );
    }

    @Test
    public void selectIncompleteIdentifier3() {
        // no closing square bracket, but for a number, but after a legal variant
        final String in =   """
                            unread = { $value ->
                                [val1] value 1.
                                [34
                            """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 0 ) );
    }


    @Test
    public void selectNoDefault() {
        // no default, AND no closing brace
        final String in =   """
                            unread = { $value ->
                                [val1] value 1.
                                [34] a number literal
                            """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0010, 0 ) );
    }


    @Test
    public void selectNoDefault2() {
        // no default, but with closing brace (so error is not at EOF)
        final String in =   """
                            unread = { $value ->
                                [val1] value 1.
                                [34] a number literal
                                
                            }
                            """;

        final FluentResource resource = parse( in );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0010, 5 ) );
    }

    @Test
    public void invalidComment() {
        final String in =   """
                            # This comment is OK (space between '#' and comment text).
                            #This comment is NOT OK, and fails parsing, but only if comments are NOT ignored.
                            # apparently this is per spec.
                            message = This is a message.
                            """;
        // Comments NOT ignored -- 1 error
        FluentResource resource = FTLParser.parse( FTLStream.of( in ), false );
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 2 ) );

        // Comments ignored -- no errors.
        resource = FTLParser.parse( FTLStream.of( in ), true );
        assertEquals( 0, resource.errors().size() );
    }

    @Test
    public void literalErrors() {
        // as:number  the word 'number' is a string literal, and must be in quotation marks.
        final String in =   """
                            msg_ok = |{BOOLEAN("This is a String", as:"number")}|
                            msg_bad_option_literal = |{BOOLEAN("This is a String", as:number)}|
                            """;
        // Comments NOT ignored -- 1 error
        final FluentResource resource = FTLParser.parse( FTLStream.of( in ), true );
        System.out.println(resource);
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0032, 2 ) );
    }

}