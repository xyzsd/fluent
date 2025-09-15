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
import fluent.syntax.AST.Commentary;
import fluent.syntax.AST.Junk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.FTLTestUtils;

import java.io.IOException;

import static fluent.syntax.parser.ParseException.ErrorCode.E0004;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class eofJunkTest {

    static final String RESOURCE = "fixtures/eof_junk.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 3 ) );
    }

    @Test
    public void verifyEntries() {
        // comments count as entries too!
        assertEquals( 1, resource.entries().size() );
    }


    @Test
    public void verifyResourceComment() {
        final String EXPECTED = "NOTE: Disable final newline insertion when editing this file.";

        resource.entries().stream()
                .filter( Commentary.ResourceComment.class::isInstance )
                .map( Commentary.ResourceComment.class::cast )
                .map( Commentary.ResourceComment::text )
                .filter( EXPECTED::equals )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "Mismatch" ) );
    }

    @Test
    public void verifyJunk() {
        final String EXPECTED = "000";

        resource.junk().stream()
                .findFirst()    // just one junk entry here
                .map( Junk::content )
                .filter( EXPECTED::equals )
                .orElseThrow( () -> new AssertionError( "Mismatch" ) );
    }


}