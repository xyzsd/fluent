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

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LeadingDotsTest {

    static final String RESOURCE = "fixtures/leading_dots.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 6, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 16 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 20 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 24 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0012, 57 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 64 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 69 ) );
    }

    @Test
    public void key01() {
        assertEquals(
                ".Value",
                FTLTestUtils.fmt( bundle, "key01" )
        );
    }

    @Test
    public void key02() {
        assertEquals(
                "…Value",
                FTLTestUtils.fmt( bundle, "key02" )
        );
    }

    @Test
    public void key03() {
        assertEquals(
                ".Value",
                FTLTestUtils.fmt( bundle, "key03" )
        );
    }

    @Test
    public void key04() {
        assertEquals(
                ".Value",
                FTLTestUtils.fmt( bundle, "key04" )
        );
    }

    @Test
    public void key05() {
        assertEquals(
                "Value\n.Continued",
                FTLTestUtils.fmt( bundle, "key05" )
        );
    }

    @Test
    public void key06() {
        assertEquals(
                ".Value\n.Continued",
                FTLTestUtils.fmt( bundle, "key06" )
        );
    }

    @Test
    public void key10() {
        assertEquals(
                "which is an attribute\nContinued",
                FTLTestUtils.attr( bundle, "key10", "Value" )
        );
    }

    @Test
    public void key11() {
        assertEquals(
                ".Value = which looks like an attribute\nContinued",
                FTLTestUtils.fmt( bundle, "key11" )
        );
    }

    @Test
    public void key12() {
        assertEquals(
                "A",
                FTLTestUtils.attr( bundle, "key12", "accesskey" )
        );
    }

    @Test
    public void key13() {
        assertEquals(
                ".Value",
                FTLTestUtils.attr( bundle, "key13", "attribute" )
        );
    }

    @Test
    public void key14() {
        assertEquals(
                ".Value",
                FTLTestUtils.attr( bundle, "key14", "attribute" )
        );
    }

    @Test
    public void key15() {
        assertEquals(
                ".Value",
                FTLTestUtils.fmt( bundle, "key15" )
        );
    }

    @Test
    public void key19() {
        assertEquals(
                "Value\nContinued",
                FTLTestUtils.attr( bundle, "key19", "attribute" )
        );
    }

    @Test
    public void key20() {
        assertEquals(
                ".Value",
                FTLTestUtils.fmt( bundle, "key20" )
        );
    }


}