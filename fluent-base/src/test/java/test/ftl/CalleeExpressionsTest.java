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

package test.ftl;import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;

import static fluent.syntax.parser.FTLParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalleeExpressionsTest {


    static final String RESOURCE = "fixtures/callee_expressions.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup(resource, false);
    }



    @Test
    public void verifyExceptions() {
        // some exceptions can occur during parsing, depending upon the input;
        // verify any exceptions that were raised are indeed present.
        assertEquals( 10, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0008, 7 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0008, 9 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 11 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0019, 13 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 15 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0008, 28 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0008, 32 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 36 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0017, 40 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 44 ) );

    }

    @Test
    public void calleesInPlaceables() {
        assertEquals(
                "{Unknown function: FUNCTION()}",
                FTLTestUtils.fmt(bundle, "function-callee-placeable")
        );

        assertEquals(
                "{Unknown term: -term}",
                FTLTestUtils.fmt(bundle, "term-callee-placeable")
        );
    }

    @Test
    public void calleesInSelectors() {
        assertEquals(
                "{Unknown function: FUNCTION()}",
                FTLTestUtils.fmt(bundle, "function-callee-selector")
        );

        assertEquals(
                "Value",    // default selector
                FTLTestUtils.fmt(bundle, "term-attr-callee-selector")
        );


    }

}
