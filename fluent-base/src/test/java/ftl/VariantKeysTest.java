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

package ftl;import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import fluent.syntax.AST.SelectExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.FTLTestUtils;

import java.io.IOException;

import static fluent.syntax.parser.ParseException.ErrorCode.E0003;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VariantKeysTest {

    static final String RESOURCE = "fixtures/variant_keys.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 3, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 24 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 30 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 36 ) );
    }

    @Test
    public void simpleIdentifier() {
        final SelectExpression se = FTLTestUtils.getSelectExpression( bundle, "simple-identifier" );
        assertEquals( "key", se.defaultVariantKey().name() );
    }

    @Test
    public void identifierSurroundedByWhitespace() {
        final SelectExpression se = FTLTestUtils.getSelectExpression( bundle, "identifier-surrounded-by-whitespace" );
        assertEquals( "key", se.defaultVariantKey().name() );
    }

    @Test
    public void intNumber() {
        final SelectExpression se = FTLTestUtils.getSelectExpression( bundle, "int-number" );
        assertEquals( "1", se.defaultVariantKey().name() );
    }

    @Test
    public void floatNumber() {
        final SelectExpression se = FTLTestUtils.getSelectExpression( bundle, "float-number" );
        assertEquals( "3.14", se.defaultVariantKey().name() );
    }

}