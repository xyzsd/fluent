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

import static fluent.syntax.parser.FTLParseException.ErrorCode.E0004;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MixedEntriesTest {

    static final String RESOURCE = "fixtures/mixed_entries.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 2, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 12 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 19 ) );
    }

    @Test
    public void checkTermBrandName() {
        assertEquals(
                "Aurora",
                FTLTestUtils.term( bundle, "brand-name", Map.of() )
        );
    }

    @Test
    public void key01() {
        assertEquals(
                "{No pattern specified for message: 'key01'}",
                FTLTestUtils.fmt( bundle, "key01" )
        );

        assertEquals(
                "Attribute",
                FTLTestUtils.attr( bundle, "key01", "attr" )
        );
    }


    @Test
    public void key02() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "key02" )
        );
    }

    @Test
    public void key03() {
        assertEquals(
                "Value 03",
                FTLTestUtils.fmt( bundle, "key03" )
        );
    }

    @Test
    public void key04() {
        assertEquals(
                "Value 04",
                FTLTestUtils.fmt( bundle, "key04" )
        );
    }

}