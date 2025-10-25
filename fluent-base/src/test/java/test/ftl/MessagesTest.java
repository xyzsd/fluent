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

import static fluent.syntax.parser.FTLParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessagesTest {

    static final String RESOURCE = "fixtures/messages.ftl";
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
        assertTrue( FTLTestUtils.matchParseException( resource, E0005, 30 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 33 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 43 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 46 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 49 ) );
    }

    @Test
    public void key01() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "key01" )
        );
    }

    @Test
    public void key02a() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "key02a" )
        );

        assertEquals(
                "Attribute",
                FTLTestUtils.attr( bundle, "key02a", "attr" )
        );
    }

    @Test
    public void key02b() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "key02b" )
        );

        assertEquals(
                "Attribute 1",
                FTLTestUtils.attr( bundle, "key02b", "attr1" )
        );

        assertEquals(
                "Attribute 2",
                FTLTestUtils.attr( bundle, "key02b", "attr2" )
        );
    }

    @Test
    public void key03() {
        assertEquals(
                "{No pattern specified for message: 'key03'}",
                FTLTestUtils.fmt( bundle, "key03" )
        );

        assertEquals(
                "Attribute",
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
                "Attribute 1",
                FTLTestUtils.attr( bundle, "key04", "attr1" )
        );

        assertEquals(
                "Attribute 2",
                FTLTestUtils.attr( bundle, "key04", "attr2" )
        );
    }

    @Test
    public void key05() {
        assertEquals(
                "{No pattern specified for message: 'key05'}",
                FTLTestUtils.fmt( bundle, "key05" )
        );

        assertEquals(
                "Attribute 1",
                FTLTestUtils.attr( bundle, "key05", "attr1" )
        );
    }

    @Test
    public void noWhitespace() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "no-whitespace" )
        );

        assertEquals(
                "Attribute 1",
                FTLTestUtils.attr( bundle, "no-whitespace", "attr1" )
        );
    }

    @Test
    public void extraWhitespace() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "extra-whitespace" )
        );

        assertEquals(
                "Attribute 1",
                FTLTestUtils.attr( bundle, "extra-whitespace", "attr1" )
        );
    }

    @Test
    public void key06() {
        assertEquals(
                "",
                FTLTestUtils.fmt( bundle, "key06" )
        );
    }

    @Test
    public void key09() {
        assertEquals(
                "Value 09",
                FTLTestUtils.fmt( bundle, "KEY09" )
        );
    }

    @Test
    public void key10() {
        assertEquals(
                "Value 10",
                FTLTestUtils.fmt( bundle, "key-10" )
        );
    }

    @Test
    public void key11() {
        assertEquals(
                "Value 11",
                FTLTestUtils.fmt( bundle, "key_11" )
        );
    }

    @Test
    public void key12() {
        assertEquals(
                "Value 12",
                FTLTestUtils.fmt( bundle, "key-12-" )
        );
    }

    @Test
    public void key13() {
        assertEquals(
                "Value 13",
                FTLTestUtils.fmt( bundle, "key_13_" )
        );
    }


}