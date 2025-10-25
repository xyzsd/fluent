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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.Map;

import static fluent.syntax.parser.FTLParseException.ErrorCode.E0003;
import static fluent.syntax.parser.FTLParseException.ErrorCode.E0006;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TermsTest {

    static final String RESOURCE = "fixtures/terms.ftl";
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
        assertTrue( FTLTestUtils.matchParseException( resource, E0006, 7 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0006, 12 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0006, 16 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0006, 20 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 23 ) );
    }

    @Test
    public void term01() {
        assertEquals(
                "Value",
                FTLTestUtils.term( bundle, "term01", Map.of() )
        );

        assertEquals(
                "Attribute",
                FTLTestUtils.termAttr( bundle, "term01", "attr", Map.of() )
        );
    }

    @Test
    public void term02() {
        assertEquals(
                "",
                FTLTestUtils.term( bundle, "term02", Map.of() )
        );
    }

    @Test
    public void term08() {
        assertEquals(
                "Value",
                FTLTestUtils.term( bundle, "term08", Map.of() )
        );

        assertEquals(
                "Attribute",
                FTLTestUtils.termAttr( bundle, "term08", "attr", Map.of() )
        );
    }

    @Test
    public void term09() {
        assertEquals(
                "Value",
                FTLTestUtils.term( bundle, "term09", Map.of() )
        );

        assertEquals(
                "Attribute",
                FTLTestUtils.termAttr( bundle, "term09", "attr", Map.of() )
        );
    }

}