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
public class StringsortFnTest {


    static final List<String> STRINGZ = List.of("alpha", "charlie", "delta", "beta", "__underscored__");
    static final String RESOURCE = "functions/stringsort_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;



    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );

        // assert that the function under test is present.
        assertTrue(bundle.registry().contains( "STRINGSORT" ) );
    }



    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    private static String fmt(String msgID, Object value) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of("value", value) );
    }


    @Test
    public void heterogeneous() {
        assertEquals(
                "|1, 2, 9999.888, four, three|",
                fmt(  "stringsort_heterogeneous", STRINGZ)
        );

    }


    @Test
    public void sortDefault() {
        assertEquals(
                "|__underscored__, alpha, beta, charlie, delta|",
                fmt(  "stringsort_default", STRINGZ)
        );
    }


    @Test
    public void sortNatural() {
        assertEquals(
                "|__underscored__, alpha, beta, charlie, delta|",
                fmt(  "stringsort_ascending", STRINGZ)
        );
    }


    @Test
    public void sortReversed() {
        assertEquals(
                "|delta, charlie, beta, alpha, __underscored__|",
                fmt(  "stringsort_descending", STRINGZ)
        );
    }

}
