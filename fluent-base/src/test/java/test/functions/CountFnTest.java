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


///  COUNT function tests
public class CountFnTest {


    static final String RESOURCE = "functions/count_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue( bundle.registry().contains( "COUNT" ) );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    @Test
    public void noValues() {
        assertEquals(
                "|0|",
                FTLTestUtils.fmt( bundle, "count_none" )
        );
    }

    @Test
    public void literals() {
        assertEquals(
                "|4|",
                FTLTestUtils.fmt( bundle, "count_literal" )
        );
    }


    @Test
    public void singleVariableSingleValue() {
        assertEquals(
                "|1|",
                FTLTestUtils.fmt( bundle, "count_one_variable", Map.of("value", "one single value") )
        );

        assertEquals(
                "|1|",
                FTLTestUtils.fmt( bundle, "count_one_variable", Map.of("value", 3) )
        );
    }

    @Test
    public void singleVariableWithList() {
        assertEquals(
                "|4|",
                FTLTestUtils.fmt( bundle, "count_one_variable",
                        Map.of( "value", List.of("a","b","c",5))
                        )
        );
    }

    @Test
    public void multipleVariables() {
        assertEquals(
                "|3|",
                FTLTestUtils.fmt( bundle, "count_multiple_variables",
                        Map.of(
                                "value_1", "the value",
                                "value_2", "another value",
                                "value_3", "the final value"
                        )
                )
        );

        // now some variables are lists
        assertEquals(
                "|9|",
                FTLTestUtils.fmt( bundle, "count_multiple_variables",
                        Map.of(
                                "value_1", List.of(1,2,3,4,5),
                                "value_2", "just a single value",
                                "value_3", List.of("a","b","c")
                        )
                )
        );
    }


    @Test
    public void mixedVariablesAndLiterals() {
        assertEquals(
                "|4|",
                FTLTestUtils.fmt( bundle, "count_mixed",
                        Map.of(
                                "value_1", "the value",
                                "value_2", "another value"
                        )
                )
        );

        // now some variables are lists
        assertEquals(
                "|8|",
                FTLTestUtils.fmt( bundle, "count_mixed",
                        Map.of(
                                "value_1", List.of(1,2,3,4,5),
                                "value_2", "just a single value"
                        )
                )
        );
    }

}
