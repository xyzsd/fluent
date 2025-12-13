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

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import fluent.syntax.parser.FTLParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.Map;

import static fluent.syntax.parser.FTLParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

///  Miscellaneous Select tests.
public class MiscSelectTest {

    static final String RESOURCE = "misc/misc_select.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, true );
    }

    enum TestEnum {
        ENUMAAA,
        ENUMBBB,
        ENUMCCC
    }

    enum TestEnumUnderscore {
        ENUM_AAA,
        ENUM_BBB,
        ENUM_CCC
    }

    @Test
    public void parseTest() {
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 27 ) );
    }

    // test select on enum values
    @Test
    public void selectEnum() {
        String result = bundle.format( "selection", Map.of( "value", TestEnum.ENUMBBB ) );
        assertEquals( "The second value, ENUMBBB.", result );

    }


    @Test
    public void selectEnumCaseDoesntMatch() {
        // ENUM in select must match exactly (case sensitive)
        String result = bundle.format( "selection_lowercase", Map.of( "value", TestEnum.ENUMBBB ) );
        // no match, so the default selection is returned.
        assertEquals( "The first and default value, ENUMAAA.", result );
    }

    // this works, because underscores are legal in FTL (as are hyphens, though hyphens are not legal in a Java enum)
    // BUT not if the identifier STARTS with an underscore.
    @Test
    public void selectEnumUnderscore() {
        // ENUM in select must match exactly (case sensitive)
        String result = bundle.format( "selection_underscore", Map.of( "value", TestEnumUnderscore.ENUM_BBB ) );
        assertEquals( "The second value, ENUM_BBB.", result );
    }
}