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

package ftl;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.FTLTestUtils;

import java.io.IOException;

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class escapedCharactersTest {

    static final String RESOURCE = "fixtures/escaped_characters.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 5, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 12 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0025, 14 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0020, 16 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0026, 31 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0026, 33 ) );
    }


    @Test
    public void literalText() {
        assertEquals(
                "Value with \\ a backslash",
                FTLTestUtils.fmt( bundle, "text-backslash-one" )
        );

        assertEquals(
                "Value with \\\\ two backslashes",
                FTLTestUtils.fmt( bundle, "text-backslash-two" )
        );

        assertEquals(
                "Value with \\{Unknown message: 'placeable'}",
                FTLTestUtils.fmt( bundle, "text-backslash-brace" )
        );

        assertEquals(
                "\\u0041",
                FTLTestUtils.fmt( bundle, "text-backslash-u" )
        );

        assertEquals(
                "\\\\u0041",
                FTLTestUtils.fmt( bundle, "text-backslash-backslash-u" )
        );
    }

    @Test
    public void stringLiterals() {
        assertEquals(
                "\"",
                FTLTestUtils.fmt( bundle, "quote-in-string" )
        );

        assertEquals(
                "\\",
                FTLTestUtils.fmt( bundle, "backslash-in-string" )
        );
    }

    @Test
    public void unicodeEscapes() {
        // \u0041 == 'A' (latin capital a)
        assertEquals(
                "A",
                FTLTestUtils.fmt( bundle, "string-unicode-4digits" )
        );

        assertEquals(
                "\\u0041",
                FTLTestUtils.fmt( bundle, "escape-unicode-4digits" )
        );

        // u01F602 = 😂 ('face with tears of joy) also as surrogate pairs: uD83DuDE02
        assertEquals(
                "\uD83D\uDE02", // hi-lo surrogate pairs (UTF16)
                FTLTestUtils.fmt( bundle, "string-unicode-6digits" )
        );

        assertEquals(
                "\\U01F602",
                FTLTestUtils.fmt( bundle, "escape-unicode-6digits" )
        );

        assertEquals(
                "A00",
                FTLTestUtils.fmt( bundle, "string-too-many-4digits" )
        );

        assertEquals(
                "\uD83D\uDE0200",
                FTLTestUtils.fmt( bundle, "string-too-many-6digits" )
        );
    }

    @Test
    public void literalBraces() {
        assertEquals(
                "An opening { brace.",
                FTLTestUtils.fmt( bundle, "brace-open" )
        );

        assertEquals(
                "A closing } brace.",
                FTLTestUtils.fmt( bundle, "brace-close" )
        );
    }


}