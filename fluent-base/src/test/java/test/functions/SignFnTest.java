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

import static fluent.syntax.parser.ParseException.ErrorCode.E0032;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


///  SIGN function tests
public class SignFnTest {


    static final String RESOURCE = "functions/sign_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        // if we don't use 'extendedBundleSetup', the OFFSET function will not be added to the registry,
        // and we will get OFFSET(...) as the output for any function invocation.
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue(bundle.registry().contains( "SIGN" ) );
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
                fmt(  "msg_sign", "A String")
        );
    }

    @Test
    public void decimals() {
        assertEquals(
                "|negative|",
                fmt(  "msg_sign", -111.222d)
        );

        assertEquals(
                "|positive|",
                fmt(  "msg_sign", +222.333d)
        );

        assertEquals(
                "|zero|",
                fmt(  "msg_sign", -0.0d)
        );

        assertEquals(
                "|zero|",
                fmt(  "msg_sign", +0.0d)
        );

        assertEquals(
                "|zero|",
                fmt(  "msg_sign", 0.0d)
        );

        assertEquals(
                "|NaN|",
                fmt(  "msg_sign",  Double.NaN)
        );

        assertEquals(
                "|positiveInfinity|",
                fmt(  "msg_sign",  Double.POSITIVE_INFINITY)
        );

        assertEquals(
                "|negativeInfinity|",
                fmt(  "msg_sign",  Double.NEGATIVE_INFINITY)
        );
    }

    @Test
    public void integers() {
        assertEquals(
                "|negative|",
                fmt(  "msg_sign", -111)
        );

        assertEquals(
                "|positive|",
                fmt(  "msg_sign", +111)
        );

        assertEquals(
                "|zero|",
                fmt(  "msg_sign", 0)
        );

        assertEquals(
                "|zero|",
                fmt(  "msg_sign", -0)
        );
    }


}
