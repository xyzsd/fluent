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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


///  ABS function tests
public class AbsFnTest {


    static final String RESOURCE = "functions/abs_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        // if we don't use 'extendedBundleSetup', the OFFSET function will not be added to the registry,
        // and we will get OFFSET(...) as the output for any function invocation.
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue(bundle.registry().contains( "ABS" ) );
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
        // non-numbers are passed through unchanged
        assertEquals(
                "|A String|",
                fmt(  "msg_abs", "A String")
        );
    }

    @Test
    public void decimals() {
        assertEquals(
                "|111.222|",
                fmt(  "msg_abs", -111.222d)
        );

        assertEquals(
                "|222.333|",
                fmt(  "msg_abs", +222.333d)
        );

        assertEquals(
                "|0|",
                fmt(  "msg_abs", -0.0d)
        );

        assertEquals(
                "|0|",
                fmt(  "msg_abs", +0.0d)
        );

        assertEquals(
                "|0|",
                fmt(  "msg_abs", 0.0d)
        );

        assertEquals(
                "|NaN|",
                fmt(  "msg_abs",  Double.NaN)
        );

        assertEquals(
                "|∞|",
                fmt(  "msg_abs",  Double.POSITIVE_INFINITY)
        );

        assertEquals(
                "|∞|",
                fmt(  "msg_abs",  Double.NEGATIVE_INFINITY)
        );
    }

    @Test
    public void integers() {
        assertEquals(
                "|111|",
                fmt(  "msg_abs", -111)
        );

        assertEquals(
                "|222|",
                fmt(  "msg_abs", +222)
        );

        assertEquals(
                "|0|",
                fmt(  "msg_abs", 0)
        );
    }


}
