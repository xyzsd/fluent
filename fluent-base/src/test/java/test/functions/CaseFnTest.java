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

package test.functions;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


///  CASE function tests
public class CaseFnTest {


    static final String RESOURCE = "functions/case_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue(bundle.registry().contains( "CASE" ) );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    private static String fmt(String msgID, Object value) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of("value", value) );
    }


    @Test
    public void passthrough() {
        // non-String values are passed through unchanged
        assertEquals(
                "|5|",
                fmt(  "msg_case_default", 5)
        );

        assertEquals(
                "|5.5|",
                fmt(  "msg_case_default", 5.5)
        );

        assertEquals(
                "|5, 5.5|",
                fmt(  "msg_case_default", List.of(5, 5.5))
        );
    }

    @Test
    public void invalid() {
        assertEquals(
                "|{CASE(): Named option 'custom': unrecognized. Allowed values: [UPPER, LOWER]}|",
                fmt(  "msg_case_invalid", List.of(5, 5.5))
        );
    }

    @Test
    public void unspecified() {
        assertEquals(
                "|MY TEST STRING IS THIS5382.|",
                fmt(  "msg_case_default", "My TEST string is This5382.")
        );

        assertEquals(
                "|a, b, c, d, !!, 5|",
                fmt(  "msg_case_lower", List.of("A","B","c","d","!!",5.0))
        );
    }


    @Test
    public void uppercase() {
        assertEquals(
                "|MY TEST STRING IS THIS5382.|",
                fmt(  "msg_case_upper", "My TEST string is This5382.")
        );

        assertEquals(
                "|a, b, c, d, !!, 5|",
                fmt(  "msg_case_lower", List.of("A","B","c","d","!!",5.0))
        );
    }


    @Test
    public void lowercase() {
        assertEquals(
                "|my test string is this5382.|",
                fmt(  "msg_case_lower", "My TEST string is This5382.")
        );

        assertEquals(
                "|a, b, c, d, !!, 5|",
                fmt(  "msg_case_lower", List.of("A","B","c","d","!!",5.0))
        );
    }


    @Test
    public void multi() {
        assertEquals(
                "|A, B, C, D, E, F|",
                FTLTestUtils.fmt(  bundle, "msg_case_multi", Map.of(
                        "value_1", List.of("a","B","c"),
                        "value_2", List.of("d","E","F")
                ) )
        );
    }
}
