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

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumbersTest {

    static final String RESOURCE = "fixtures/numbers.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );

        resource.entries().forEach( entry -> {System.out.println(entry);});
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 7, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 32 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 33 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 34 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 35 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 36 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 37 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 38 ) );
    }

    @Test
    public void intGroup() {
        assertEquals(
                "0",
                FTLTestUtils.fmt( bundle, "int-zero" )
        );

        assertEquals(
                "1",
                FTLTestUtils.fmt( bundle, "int-positive" )
        );

        assertEquals(
                "-1",
                FTLTestUtils.fmt( bundle, "int-negative" )
        );

        assertEquals(
                "0",
                FTLTestUtils.fmt( bundle, "int-negative-zero" )
        );
    }


    @Test
    public void intGroupPadded() {
        assertEquals(
                "1",
                FTLTestUtils.fmt( bundle, "int-positive-padded" )
        );

        assertEquals(
                "-1",
                FTLTestUtils.fmt( bundle, "int-negative-padded" )
        );

        assertEquals(
                "0",
                FTLTestUtils.fmt( bundle, "int-zero-padded" )
        );

        assertEquals(
                "0",
                FTLTestUtils.fmt( bundle, "int-negative-zero-padded" )
        );
    }


    @Test
    public void floatGroupPositive() {
        assertEquals(
                "0",
                FTLTestUtils.fmt( bundle, "float-zero" )
        );

        assertEquals(
                "0.01",
                FTLTestUtils.fmt( bundle, "float-positive" )
        );

        assertEquals(
                "1.03",
                FTLTestUtils.fmt( bundle, "float-positive-one" )
        );

        assertEquals(
                "1",
                FTLTestUtils.fmt( bundle, "float-positive-without-fraction" )
        );
    }

    @Test
    public void floatGroupNegative() {
        assertEquals(
                "-0.01",
                FTLTestUtils.fmt( bundle, "float-negative" )
        );

        assertEquals(
                "-1.03",
                FTLTestUtils.fmt( bundle, "float-negative-one" )
        );

        assertEquals(
                "-0",
                FTLTestUtils.fmt( bundle, "float-negative-zero" )
        );

        assertEquals(
                "-1",
                FTLTestUtils.fmt( bundle, "float-negative-without-fraction" )
        );
    }

    @Test
    public void floatGroupPositivePadded() {
        assertEquals(
                "1.03",
                FTLTestUtils.fmt( bundle, "float-positive-padded-left" )
        );

        assertEquals(
                "1.03",
                FTLTestUtils.fmt( bundle, "float-positive-padded-right" )
        );

        assertEquals(
                "1.03",
                FTLTestUtils.fmt( bundle, "float-positive-padded-both" )
        );
    }

    @Test
    public void floatGroupNegativePadded() {
        assertEquals(
                "-1.03",
                FTLTestUtils.fmt( bundle, "float-negative-padded-left" )
        );

        assertEquals(
                "-1.03",
                FTLTestUtils.fmt( bundle, "float-negative-padded-right" )
        );

        assertEquals(
                "-1.03",
                FTLTestUtils.fmt( bundle, "float-negative-padded-both" )
        );
    }


}