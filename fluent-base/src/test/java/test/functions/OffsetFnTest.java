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
import java.math.BigDecimal;
import java.util.Map;

import static fluent.syntax.parser.ParseException.ErrorCode.E0003;
import static fluent.syntax.parser.ParseException.ErrorCode.E0032;
import static org.junit.jupiter.api.Assertions.*;


///  OFFSET function tests
public class OffsetFnTest {


    static final String RESOURCE = "functions/offset_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        // if we don't use 'extendedBundleSetup', the OFFSET function will not be added to the registry,
        // and we will get OFFSET(...) as the output for any function invocation.
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue(bundle.registry().contains( "OFFSET" ) );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 1, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0032, 6 ) );
    }

    ///  simplify simple formats; 'value' is always the name of the assigned variable.
    private static String fmt(String msgID) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of() );
    }

    private static String fmt(String msgID, Object value) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of("value", value) );
    }

    private static String fmt(String msgID, Object value, Object offset) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of("value", value, "offset", offset) );
    }


    @Test
    public void errorStringLiteral() {
        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentString<String>; expected a FluentLong}|",
                fmt(  "msg_string_inc")
        );

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentString<String>; expected a FluentLong}|",
                fmt(  "msg_string_dec")
        );
    }


    @Test
    public void errorFloatLiteral() {
        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentDouble<Double>; expected a FluentLong}|",
                fmt(  "msg_float_inc")
        );

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentDouble<Double>; expected a FluentLong}|",
                fmt(  "msg_float_dec")
        );

    }

    @Test
    public void errorFloatIncrementOrDecrement() {
        assertEquals(
                "|{OFFSET(): Named option 'increment': expected type Long (actual: '3.5')}|",
                fmt(  "msg_invalid_inc_f")
        );

        assertEquals(
                "|{OFFSET(): Named option 'decrement': expected type Long (actual: '3.5')}|",
                fmt(  "msg_invalid_dec_f")
        );

    }

    @Test
    public void errorStringIncrementOrDecrement() {
        assertEquals(
                "|{OFFSET(): Named option 'increment': expected type Long (actual: 'ALiteralString')}|",
                fmt(  "msg_invalid_inc_s")
        );

        assertEquals(
                "|{OFFSET(): Named option 'decrement': expected type Long (actual: 'ALiteralString')}|",
                fmt(  "msg_invalid_dec_s")
        );

    }


    @Test
    public void errorTooManyOptions() {
        assertEquals(
                "|{OFFSET(): Expected exactly one option, either 'increment' or 'decrement'}|",
                fmt(  "msg_invalid_badoptions", Boolean.TRUE)
        );
    }

    @Test
    public void errorInvalidValues() {

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentString<String>; expected a FluentLong}|",
                fmt(  "msg_offset_inc", "AString")
        );

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentDouble<Double>; expected a FluentLong}|",
                fmt(  "msg_offset_inc", 3.14f)
        );

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentCustom<Boolean>; expected a FluentLong}|",
                fmt(  "msg_offset_inc", Boolean.TRUE)
        );

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentString<String>; expected a FluentLong}|",
                fmt(  "msg_offset_dec", "AString")
        );

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentDouble<Double>; expected a FluentLong}|",
                fmt(  "msg_offset_dec", 3.14f)
        );

        assertEquals(
                "|{OFFSET(): Invalid type: received a FluentCustom<Boolean>; expected a FluentLong}|",
                fmt(  "msg_offset_dec", Boolean.TRUE)
        );


    }

    @Test
    public void fixedIncrementDecrement() {
        assertEquals(
                "|90|",
                fmt(  "msg_offset_dec", 100)
        );

        assertEquals(
                "|110|",
                fmt(  "msg_offset_inc", 100)
        );

    }
    /*



msg_offset_variable = |OFFSET($value, increment:$increment)|
     */

}
