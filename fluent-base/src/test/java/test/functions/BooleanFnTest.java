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


/// BOOLEAN function tests
///
/// This is different from other functions, because it operates on FluentCustoms that contain Booleans.
///
public class BooleanFnTest {


    static final String RESOURCE = "functions/boolean_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue( bundle.registry().contains( "BOOLEAN" ) );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    @Test
    public void noValues() {
        assertEquals(
                "|{BOOLEAN(): No positional arguments supplied; at least one is required.}|",
                FTLTestUtils.fmt( bundle, "msg_no_booleans" )
        );
    }

    @Test
    public void notABoolean() {
        // String is passed through
        assertEquals(
                "|false|",
                FTLTestUtils.fmt( bundle, "msg_not_boolean" )
        );

        assertEquals(
                "|This is a String|",
                FTLTestUtils.fmt( bundle, "msg_not_boolean_2" )
        );

        assertEquals(
                "|This is a String|",
                FTLTestUtils.fmt( bundle, "msg_not_boolean_3" )
        );
    }

    @Test
    public void singleVariableSingleValue() {
        assertEquals(
                "|true|",
                FTLTestUtils.fmt( bundle, "msg_boolean",
                        Map.of(
                                "value", Boolean.TRUE
                        )
                )
        );

        assertEquals(
                "|false|",
                FTLTestUtils.fmt( bundle, "msg_boolean",
                        Map.of(
                                "value", false
                        )
                )
        );
    }

    @Test
    public void singleVariableList() {
        assertEquals(
                "|true, false, true, false|",
                FTLTestUtils.fmt( bundle, "msg_boolean",
                        Map.of(
                                "value", List.of(true, false, true, false)
                        )
                )
        );
    }

    @Test
    public void msg_boolean_as_string() {
        assertEquals(
                "|true, false, true, false|",
                FTLTestUtils.fmt( bundle, "msg_boolean_as_string",
                        Map.of(
                                "value", List.of(true, false, true, false)
                        )
                )
        );
    }

    @Test
    public void msg_boolean_as_number() {
        assertEquals(
                "|1, 0, 1, 0|",
                FTLTestUtils.fmt( bundle, "msg_boolean_as_number",
                        Map.of(
                                "value", List.of(true, false, true, false)
                        )
                )
        );
    }

    @Test
    public void multipleVariables() {
        assertEquals(
                "|true, false|",
                FTLTestUtils.fmt( bundle, "msg_multiple_variables",
                        Map.of(
                                "value_1", Boolean.TRUE,
                                "value_2", Boolean.FALSE
                        )
                )
        );

        assertEquals(
                "|true, true, false, true, false|",
                FTLTestUtils.fmt( bundle, "msg_multiple_variables",
                        Map.of(
                                "value_1", Boolean.TRUE,
                                "value_2", List.of(true, false, true, false)
                        )
                )
        );
    }

    @Test
    public void msg_boolean_adjusted() {
        assertEquals(
                "|11|",
                FTLTestUtils.fmt( bundle, "msg_boolean_adjusted",
                        Map.of(
                                "value", Boolean.TRUE
                        )
                )
        );

        assertEquals(
                "|10|",
                FTLTestUtils.fmt( bundle, "msg_boolean_adjusted",
                        Map.of(
                                "value", Boolean.FALSE
                        )
                )
        );

        assertEquals(
                "|11, 10, 11, 10|",
                FTLTestUtils.fmt( bundle, "msg_boolean_adjusted",
                        Map.of(
                                "value", List.of(true, false, true, false)
                        )
                )
        );
    }


    @Test
    public void booleanSelect() {
        assertEquals(
                "|Success!|",
                FTLTestUtils.fmt( bundle, "select_boolean",
                        Map.of("value", true)
                )
        );

        assertEquals(
                "|Failure.|",
                FTLTestUtils.fmt( bundle, "select_boolean",
                        Map.of("value", false)
                )
        );
    }

}
