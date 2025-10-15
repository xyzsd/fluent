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

package test.ftl;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import fluent.syntax.AST.Junk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JunkTest {

    static final String RESOURCE = "fixtures/junk.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }

    @Test
    public void verifyExceptions() {
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 3 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 4 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 7 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 11 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 16 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 21 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 22 ) );
        assertEquals( 7, resource.errors().size() );
    }


    @Test
    public void junkAdjacent1() {
        final String EXPECTED = "err01 = {1x}\n";
        extracted( EXPECTED );
    }

    @Test
    public void junkAdjacent2() {
        final String EXPECTED = "err02 = {2x}\n\n";
        extracted( EXPECTED );
    }

    @Test
    public void junkSingle1() {
        final String EXPECTED = "err03 = {1x\n2\n\n";
        extracted( EXPECTED );
    }

    @Test
    public void junkSingle2() {
        final String EXPECTED = "ą=Invalid identifier\nć=Another one\n\n";
        extracted( EXPECTED );
    }

    private static void extracted(final String expected) {
        resource.junk().stream()
                .map( Junk::content )
                .filter( expected::equals )
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void junkErr04_both() {
        // this handles both 'err04' junk entries
        final String EXPECTED = "err04 = {\n";
        final long count = resource.junk().stream()
                .map( Junk::content )
                .filter( EXPECTED::equals )
                .count();

        assertEquals( 2L, count );
    }

    @Test
    public void junkSeparateClosingBrace() {
        String found = resource.junk().stream()
                .map( Junk::content )
                .filter( x -> x.startsWith( "}\n" ) )
                .findFirst()
                .orElseThrow();

        final String EXPECTED = "}\n";
        extracted( EXPECTED );
    }
}