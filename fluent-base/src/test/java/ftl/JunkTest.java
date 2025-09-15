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
import fluent.syntax.AST.Junk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.FTLTestUtils;

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

        System.out.println( "----" );
        resource.junk().forEach( System.out::println );
        System.out.println( "----" );
    }

    @Test
    public void verifyExceptions() {
        assertEquals( 7, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 2 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 3 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 6 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 10 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 15 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 20 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 21 ) );
    }


    @Test
    public void junkAdjacent1() {
        final String EXPECTED = "err01 = {1x}\n";
        resource.junk().stream()
                .map( Junk::content )
                .filter( EXPECTED::equals )
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void junkAdjacent2() {
        final String EXPECTED = "err02 = {2x}\n\n";
        resource.junk().stream()
                .map( Junk::content )
                .filter( EXPECTED::equals )
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void junkSingle1() {
        final String EXPECTED = "err03 = {1x\n2\n\n";
        resource.junk().stream()
                .map( Junk::content )
                .filter( EXPECTED::equals )
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void junkSingle2() {
        final String EXPECTED = "ą=Invalid identifier\nć=Another one\n\n";
        resource.junk().stream()
                .map( Junk::content )
                .filter( EXPECTED::equals )
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
        // todo: actual is "}\n"+2 consecutive nulls (!)

                String found =         resource.junk().stream()
                .map( Junk::content )
                .filter( x -> x.startsWith("}\n") )
                .findFirst()
                .orElseThrow();

        System.out.println( found );
        for(int i=0; i<found.length(); i++) {
            System.out.printf("%d : %d '%s'\n",i,found.codePointAt( i ),found.charAt(i));
        }

        final String EXPECTED = "}\n";
        resource.junk().stream()
                .map( Junk::content )
                .filter( EXPECTED::equals )
                .findFirst()
                .orElseThrow();
    }
}