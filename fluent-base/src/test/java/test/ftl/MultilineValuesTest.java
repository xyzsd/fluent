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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultilineValuesTest {

    static final String RESOURCE = "fixtures/multiline_values.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }

    @Test
    public void key01() {
        assertEquals(
                "A multiline value\ncontinued on the next line\n\nand also down here.",
                FTLTestUtils.fmt( bundle, "key01" )
        );
    }


    @Test
    public void key02() {
        assertEquals(
                "A multiline value starting\non a new line.",
                FTLTestUtils.fmt( bundle, "key02" )
        );
    }

    @Test
    public void key03() {
        assertEquals(
                "{No pattern specified for message: 'key03'}",
                FTLTestUtils.fmt( bundle, "key03" )
        );

        assertEquals(
                "A multiline attribute value\ncontinued on the next line\n\nand also down here.",
                FTLTestUtils.attr( bundle, "key03", "attr" )
        );
    }

    @Test
    public void key04() {
        assertEquals(
                "{No pattern specified for message: 'key04'}",
                FTLTestUtils.fmt( bundle, "key04" )
        );

        assertEquals(
                "A multiline attribute value\nstaring on a new line",
                FTLTestUtils.attr( bundle, "key04", "attr" )
        );
    }


    @Test
    public void key05() {
        assertEquals(
                "A multiline value with non-standard\n\n    indentation.",
                FTLTestUtils.fmt( bundle, "key05" )
        );
    }

    @Test
    public void key06() {
        assertEquals(
                "A multiline value with placeables\nat the beginning and the end\nof lines.",
                FTLTestUtils.fmt( bundle, "key06" )
        );
    }

    @Test
    public void key07() {
        assertEquals(
                "A multiline value starting and ending with a placeable",
                FTLTestUtils.fmt( bundle, "key07" )
        );
    }

    @Test
    public void key08() {
        assertEquals(
                "Leading and trailing whitespace.",
                FTLTestUtils.fmt( bundle, "key08" )
        );
    }

    @Test
    public void key09() {
        assertEquals(
                "zero\n   three\n  two\n one\nzero",
                FTLTestUtils.fmt( bundle, "key09" )
        );
    }

    @Test
    public void key10() {
        assertEquals(
                "  two\nzero\n    four",
                FTLTestUtils.fmt( bundle, "key10" )
        );
    }

    @Test
    public void key11() {
        assertEquals(
                "  two\nzero",
                FTLTestUtils.fmt( bundle, "key11" )
        );
    }

    @Test
    public void key12() {
        assertEquals(
                ".\n    four",
                FTLTestUtils.fmt( bundle, "key12" )
        );
    }

    @Test
    public void key13() {
        assertEquals(
                "    four\n.",
                FTLTestUtils.fmt( bundle, "key13" )
        );
    }

}