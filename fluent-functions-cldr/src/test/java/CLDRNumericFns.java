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

import fluent.functions.Options;
import fluent.functions.cldr.CLDRFunctionFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// CLDR numeric function tests
public class CLDRNumericFns {


    static final CommonTest t = CommonTest.init(
            CLDRFunctionFactory.INSTANCE,
            Locale.US,
            true
    );


    // ABS tests
    @Test
    void testABS() {
        String ftl = """
                fn0 = { ABS() }
                fn1 = { ABS("of steel") }
                fn2 = { ABS(-5) }
                fn3 = { ABS(0) }
                fn4 = { ABS(5) }
                fn5 = { ABS(-123.456) }
                fn6 = { ABS(456.789) }
                fn7 = { ABS(-0.0) }
                """;

        // no args? error
        assertEquals( "{ABS()}", t.msg( ftl, "fn0" ) );

        // non-numeric? passthrough
        assertEquals( "of steel", t.msg( ftl, "fn1" ) );

        assertEquals( "5", t.msg( ftl, "fn2" ) );
        assertEquals( "0", t.msg( ftl, "fn3" ) );
        assertEquals( "5", t.msg( ftl, "fn4" ) );
        assertEquals( "123.456", t.msg( ftl, "fn5" ) );
        assertEquals( "456.789", t.msg( ftl, "fn6" ) );
        assertEquals( "0", t.msg( ftl, "fn7" ) );
    }

    // SIGN tests
    @Test
    void testSIGN() {
        String ftl = """
                    fn0 = { SIGN() }
                    fn1 = { SIGN("sagittarius") }
                    fn2 = { SIGN(-5) }
                    fn3 = { SIGN(0) }
                    fn4 = { SIGN(5) }
                    fn5 = { SIGN(-123.456) }
                    fn6 = { SIGN(456.789) }
                    fn7 = { SIGN(-0.0) }
                    fn8 = { SIGN($num) }
                    """;

        // no args? error
        assertEquals( "{SIGN()}", t.msg( ftl, "fn0" ) );

        // non-numeric? passthrough
        assertEquals( "sagittarius", t.msg( ftl, "fn1" ) );

        // typicals
        assertEquals( "negative", t.msg( ftl, "fn2" ) );
        assertEquals( "zero", t.msg( ftl, "fn3" ) );
        assertEquals( "positive", t.msg( ftl, "fn4" ) );
        assertEquals( "negative", t.msg( ftl, "fn5" ) );
        assertEquals( "positive", t.msg( ftl, "fn6" ) );
        assertEquals( "zero", t.msg( ftl, "fn7" ) );

        // non-finite
        assertEquals( "negativeInfinity", t.msg( ftl, "fn8",
                Map.of( "num", Double.NEGATIVE_INFINITY ) ) );
        assertEquals( "positiveInfinity", t.msg( ftl, "fn8",
                Map.of( "num", Double.POSITIVE_INFINITY ) ) );
        assertEquals( "nan", t.msg( ftl, "fn8",
                Map.of( "num", Double.NaN ) ) );
    }


    // IADD test
    @Test
    void testIADD() {
        String ftl = """
                    fn0 = { IADD() }
                    fn1 = { IADD("no, UADD") }
                    fn2 = { IADD("no, UADD", addend:1) }
                    fn3 = { IADD(5, addend:5) }
                    fn4 = { IADD(5, addend:-5) }
                    fn5 = { IADD(5, addend:2.2) }
                    fn6 = { IADD(3.3, addend:1) }
                    fn7 = { IADD($list, addend:10) }
                    """;

        assertEquals( "{IADD()}", t.msg( ftl, "fn0" ) );

        assertEquals( "{IADD()}", t.msg( ftl, "fn1" ) );
        assertEquals( "{IADD()}", t.msg( ftl, "fn2" ) );

        assertEquals( "10", t.msg( ftl, "fn3" ) );
        assertEquals( "0", t.msg( ftl, "fn4" ) );

        assertEquals( "{IADD()}", t.msg( ftl, "fn5" ) );
        assertEquals( "{IADD()}", t.msg( ftl, "fn6" ) );

        assertEquals( "20, 21, 22", t.msg( ftl, "fn7",
                Map.of("list", List.of(10, 11, 12)) ) );

    }

    @Test
    void testCURRENCY() {
        String ftl = """
                    fn0 = { CURRENCY() }
                    fn1 = { CURRENCY("none") }
                    fn2 = { CURRENCY(100) }
                    fn3 = { CURRENCY(123.456) }
                    fn4 = { CURRENCY(-100) }
                    """;

        assertEquals( "{CURRENCY()}", t.msg( ftl, "fn0" ) );
        assertEquals( "none", t.msg( ftl, "fn1" ) );
        assertEquals( "$100.00", t.msg( ftl, "fn2" ) );
        assertEquals( "$123.46", t.msg( ftl, "fn3" ) );
        assertEquals( "-$100.00", t.msg( ftl, "fn4" ) );
    }

    @Test
    void testCOMPACT() {
        String ftl = """
                    fn0 = { COMPACT() }
                    fn1 = { COMPACT("none") }
                    fn2 = { COMPACT(10000) }
                    fn3 = { COMPACT(10000,style:"short") }
                    fn4 = { COMPACT(10000,style:"long") }
                    fn5 = { COMPACT(10800,style:"short") }
                    """;

        assertEquals( "{COMPACT()}", t.msg( ftl, "fn0" ) );
        assertEquals( "none", t.msg( ftl, "fn1" ) );
        assertEquals( "10K", t.msg( ftl, "fn2" ) );
        assertEquals( "10K", t.msg( ftl, "fn3" ) );
        assertEquals( "10 thousand", t.msg( ftl, "fn4" ) );
        assertEquals( "11K", t.msg( ftl, "fn5" ) );
    }

    @Test
    void testDECIMAL() {
        String ftl = """
                fn0 = { DECIMAL() }
                fn1 = { DECIMAL("hello") }
                fn2 = { DECIMAL(12345.6789) }
                fn3 = { DECIMAL(-12345.6789,pattern:"#0.000;(#0.000)") }
                fn4 = { DECIMAL(12345.6789,pattern:"#0.000;(#0.000)") }
                fn5 = { DECIMAL($list,pattern:"#.#") }
                """;

        assertEquals( "{DECIMAL()}", t.msg( ftl, "fn0" ) );
        assertEquals( "hello", t.msg( ftl, "fn1" ) );
        assertEquals( "12,345.679", t.msg( ftl, "fn2" ) );
        assertEquals( "(12345.679)", t.msg( ftl, "fn3" ) );
        assertEquals( "12345.679", t.msg( ftl, "fn4" ) );

        assertEquals(
                "-100, 100, 500.3, 3871.3, 0",
                t.msg(
                        ftl,
                        "fn5" ,
                        Map.of("list",
                                List.of(-100, 100, 500.34d, 3871.343f, 0.0d))
                )
        );
    }

    @Test
    void testOptions() {
        // these are 'global' options, and will apply to any function that uses
        // these option parameters.
        Options opts = Options.builder()
                .set( "minimumFractionDigits", 4 )
                .set( "maximumFractionDigits", 5 )
                .build();

        String ftl = "fn0 = { $num }";

        assertEquals(
                "1.2300",
                t.msg( ftl, "fn0", Map.of("num",new BigDecimal("1.23")), opts )
        );

        assertEquals(
                "1.23457",  // note rounding
                t.msg( ftl, "fn0", Map.of("num",new BigDecimal("1.23456789")), opts )
        );
    }
}
