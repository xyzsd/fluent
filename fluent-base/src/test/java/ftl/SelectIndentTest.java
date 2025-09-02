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

import java.io.IOException;
import java.util.Map;

import static fluent.syntax.parser.ParseException.ErrorCode.E0003;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectIndentTest {

    static final String RESOURCE = "fixtures/select_indent.ftl";
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
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 70 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 70 ) );
    }

    @Test
    public void select1tbsInline() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-1tbs-inline" )
        );
    }

    @Test
    public void select1tbsNewline() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-1tbs-newline" )
        );
    }

    @Test
    public void select1tbsIndent() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-1tbs-indent" )
        );
    }

    @Test
    public void selectAllmanInline() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-allman-inline" )
        );

        assertEquals(
                "Other",
                FTLTestUtils.fmt( bundle, "select-allman-inline", Map.of( "selector", "other" ) )
        );
    }

    @Test
    public void selectAllmanNewline() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-allman-newline" )
        );
    }

    @Test
    public void selectAllmanIndent() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-allman-indent" )
        );
    }

    @Test
    public void selectGNUInline() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-gnu-inline" )
        );
    }

    @Test
    public void selectGNUNewline() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-gnu-newline" )
        );
    }

    @Test
    public void selectGNUIndent() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-gnu-indent" )
        );
    }

    @Test
    public void selectNoIndent() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-no-indent" )
        );

        assertEquals(
                "Other",
                FTLTestUtils.fmt( bundle, "select-allman-inline", Map.of( "selector", "other" ) )
        );
    }

    @Test
    public void selectNoIndentMultiline() {
        assertEquals(
                "Value\nContinued",
                FTLTestUtils.fmt( bundle, "select-no-indent-multiline" )
        );

        assertEquals(
                "Other\nMultiline",
                FTLTestUtils.fmt( bundle, "select-no-indent-multiline", Map.of( "selector", "other" ) )
        );
    }

    @Test
    public void selectFlat() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-flat" )
        );

        assertEquals(
                "Other",
                FTLTestUtils.fmt( bundle, "select-allman-inline", Map.of( "selector", "other" ) )
        );
    }

    @Test
    public void selectFlatWithTrailingSpaces() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "select-flat-with-trailing-spaces" )
        );

        assertEquals(
                "Other",
                FTLTestUtils.fmt( bundle, "select-allman-inline", Map.of( "selector", "other" ) )
        );
    }

}