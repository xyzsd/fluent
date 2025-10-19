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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


///  NUMSORT function tests
public class NumsortFnTest {


    static final List<Number> NUMBERZ = List.of(38, -381, 0.0d,
            new BigDecimal( "8319481938746134.89177777" ),
            new BigDecimal( "0.00000000000000000000013579" ),   // this will print as 0 unless we specify different formatting
              139584.832934, -3819.3819);
    static final String RESOURCE = "functions/numsort_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;



    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue(bundle.registry().contains( "NUMSORT" ) );
    }



    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    private static String fmt(String msgID, Object value) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of("value", value) );
    }


    @Test
    public void invalid() {
        // NO passthrough!
        assertEquals(
                "|{NUMSORT(): Expected FluentNumber<>, not non-numeric FluentValue: 'FluentString[value=string]'}|",
                fmt(  "numsort_invalid", NUMBERZ)
        );

    }


    @Test
    public void sortDefault() {
        assertEquals(
                "|-3,819.3819, -381, 0, 0, 38, 139,584.832934, 8,319,481,938,746,134.891778|",
                fmt(  "numsort_default", NUMBERZ)
        );
    }


    @Test
    public void sortAscending() {
        assertEquals(
                "|-3,819.3819, -381, 0, 0, 38, 139,584.832934, 8,319,481,938,746,134.891778|",
                fmt(  "numsort_ascending", NUMBERZ)
        );
    }


    @Test
    public void sortDescending() {
        assertEquals(
                "|8,319,481,938,746,134.891778, 139,584.832934, 38, 0, 0, -381, -3,819.3819|",
                fmt(  "numsort_descending", NUMBERZ)
        );
    }

    @Test
    public void combo() {
        assertEquals(
                "|-3.819382E3, -3.81E2, 0E0, 1.3579E-22, 3.8E1, 1.395848E5, 8.319482E15|",
                fmt(  "numsort_combo", NUMBERZ)
        );
    }
}
