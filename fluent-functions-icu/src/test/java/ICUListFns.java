/*
 *
 *  Copyright (C) 2021, xyzsd (Zach Del)
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
 */

import fluent.functions.icu.ICUFunctionFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ICUListFns {

    static final CommonTest t = CommonTest.init(
            ICUFunctionFactory.INSTANCE,
            Locale.US,
            true
    );


    @Test
    void testCOUNT() {
        String ftl = """
                fn0 = { COUNT() }
                fn1 = { COUNT(5) } 
                fn2 = { COUNT("string") }
                fn3 = { COUNT("string", 5) }
                fn4 = { COUNT($list) }
                fn5 = { COUNT($list, 5, 10) }
                """;

        assertEquals( "0", t.msg( ftl, "fn0" ) );
        assertEquals( "1", t.msg( ftl, "fn1" ) );
        assertEquals( "1", t.msg( ftl, "fn2" ) );
        assertEquals( "2", t.msg( ftl, "fn3" ) );

        Map<String,?> args = Map.of("list", List.of("a","b","c"));
        assertEquals( "3", t.msg( ftl, "fn4", args ) );
        assertEquals( "5", t.msg( ftl, "fn5", args ) );
    }


    @Test
    void testSTRINGSORT() {
        final List<String> ALPHA_6 = List.of(
                "epsilon","bEta", "alpha","zeta", "bEtA","ALPHA"
        );

        final Map<String,?> args = Map.of("list", ALPHA_6);

        String ftl = """
                fn0 = { STRINGSORT() }
                fn1 = { STRINGSORT($list) } 
                fn2 = { STRINGSORT($list, order:"reversed") }
                fn3 = { STRINGSORT($list, strength:"tertiary") }
                """;


        assertEquals( "{STRINGSORT()}", t.msg( ftl, "fn0" ) );
        assertEquals( "alpha, ALPHA, bEta, bEtA, epsilon, zeta", t.msg( ftl, "fn1", args ) );
        assertEquals( "zeta, epsilon, bEta, bEtA, alpha, ALPHA", t.msg( ftl, "fn2", args) );
        assertEquals( "alpha, ALPHA, bEta, bEtA, epsilon, zeta", t.msg( ftl, "fn3", args ) );

    }

    @Test
    void testNUMSORT() {
        final List<Number> NUMLIST = List.of(
                3184L,
                538754L,
                1734.3489d,
                193547.37771d,
                0L,
                0.0d,
                new BigDecimal( "193547.37772" ),
                new BigDecimal( "-10.000001000" ),
                new BigDecimal( ".00000120" )
        );

        final Map<String,?> args = Map.of("list", NUMLIST);

        // implicit formatting and joining will be used

        String ftl = """
                fn0 = { NUMSORT() }
                fn1 = { NUMSORT($list) } 
                fn2 = { NUMSORT($list, order:"descending") }
                fn3 = { NUMBER(NUMSORT($list), minimumFractionDigits:2, useGrouping:"true") }
                """;

        assertEquals( "{NUMSORT()}", t.msg( ftl, "fn0" ) );

        // implicit number formatter omits '.0' and keeps decimals to 3 places max
        assertEquals(
                "-10, 0, 0, 0, 1,734.349, 3,184, 193,547.378, 193,547.378, 538,754",
                t.msg( ftl, "fn1", args )
        );

        assertEquals(
                "538,754, 193,547.378, 193,547.378, 3,184, 1,734.349, 0, 0, 0, -10",
                t.msg( ftl, "fn2", args )
        );

        // composed with number formatter
        assertEquals(
                "-10.00, 0.00, 0.00, 0.00, 1,734.349, 3,184.00, 193,547.378, 193,547.378, 538,754.00",
                t.msg( ftl, "fn3", args )
        );

    }




}
