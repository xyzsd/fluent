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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.FTLTestUtils;

import java.io.IOException;
import java.util.Map;

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VariablesTest {

    static final String RESOURCE = "fixtures/variables.ftl";
    static FluentResource resource;
    static FluentBundle bundle;
    static final String KEY = "var";
    static final String VALUE = "VALUE";
    static final Map<String,Object> VAR_MAP = Map.of(KEY,VALUE);

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 3, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 13 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 15 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 17 ) );
    }

    @Test
    public void key01() {
        assertEquals(
                VALUE,
                FTLTestUtils.fmt( bundle, "key01", VAR_MAP )
        );
    }


    @Test
    public void key02() {
        assertEquals(
                VALUE,
                FTLTestUtils.fmt( bundle, "key02", VAR_MAP )
        );

    }

    @Test
    public void key03() {
        assertEquals(
                VALUE,
                FTLTestUtils.fmt( bundle, "key03", VAR_MAP )
        );

    }

    @Test
    public void key04() {
        assertEquals(
                VALUE,
                FTLTestUtils.fmt( bundle, "key04", VAR_MAP )
        );

    }
}