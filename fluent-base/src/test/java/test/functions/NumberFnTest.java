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

import static org.junit.jupiter.api.Assertions.assertEquals;


///  This is a basic test of essential Number options
public class NumberFnTest {


    static final String RESOURCE = "functions/number_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );

        resource.errors().forEach( error -> System.out.println(error.getMessage()) );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    ///  simplify simple formats; 'value' is always the name of the assigned variable.
    private static String fmt(String msgID, Number value) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of("value", value) );
    }

    private static String fmtBD(String msgID, String value) {
        return FTLTestUtils.fmt( bundle, msgID, Map.of("value", new BigDecimal( value )) );
    }


    @Test
    public void implicitIntegrals() {
        assertEquals(
                "|38,190|",
                fmt(  "msg_implicit", 38190)
        );

        assertEquals(
                "|138,729,347,234|",
                fmt(  "msg_implicit", 138729347234L)
        );

        assertEquals(
                "|12,345|",
                fmtBD(  "msg_implicit",  "00012345" )
        );
    }

    @Test
    public void implicitDecimals() {
        assertEquals(
                "|505.549988|",
                fmt(  "msg_implicit", 505.550f)
        );

        assertEquals(
                "|505.55|",
                fmt(  "msg_implicit", 505.550d)
        );

        assertEquals(
                "|12,345,678.123457|",
                fmt(  "msg_implicit", 12345678.12345678d)
        );

        assertEquals(
                "|12,340.01234|",
                fmtBD(  "msg_implicit",  "012340.012340" )
        );
    }

    @Test
    public void minimumDigits() {
        assertEquals(
                "|12,345,678.123457|",
                fmt(  "min_integer_a", 12345678.12345678d)
        );

        // no preceding 0
        assertEquals(
                "|.123457|",
                fmt(  "min_integer_a", 0.12345678d)
        );

        assertEquals(
                "|12,345,678.123457|",
                fmt(  "min_integer_b", 12345678.12345678d)
        );

        assertEquals(
                "|0,000.123457|",
                fmt(  "min_integer_b", 0.12345678d)
        );
    }

    @Test
    public void useGrouping() {
        assertEquals(
                "|123,456,789|",
                fmt(  "grouping_a", 123456789)
        );

        assertEquals(
                "|123456789|",
                fmt(  "grouping_b", 123456789)
        );

        assertEquals(
                "|123,456,789|",
                fmt(  "grouping_c", 123456789)
        );
    }

    @Test
    public void currencyStyle() {
        assertEquals(
                "|$1,000,000.00|",
                fmt(  "style_currency", 1000000)
        );

        assertEquals(
                "|1,000,000.00 US dollars|",
                fmt(  "style_currency_alt", 1000000)
        );

        assertEquals(
                "|$19.99|",
                fmt(  "style_currency", 19.99d)
        );
    }

    @Test
    public void percentStyle() {
        assertEquals(
                "|10%|",
                fmt(  "style_percent", 0.1d)
        );

        assertEquals(
                "|10 percent|",
                fmt(  "style_percent_alt", 0.1d)
        );


        assertEquals(
                "|150%|",
                fmt(  "style_percent", 1.5d)
        );

        assertEquals(
                "|123.45%|",
                fmt(  "style_percent", 1.2345d)
        );
    }


    @Test
    public void precisionFractionDigits() {
        assertEquals(
                "|123.456|",
                fmt(  "fx_min_0", 123.456d)
        );

        assertEquals(
                "|123.4560|",
                fmt(  "fx_min_4", 123.456d)
        );

        assertEquals(
                "|123|",
                fmt(  "fx_max_0", 123.456d)
        );

        assertEquals(
                "|123.46|",
                fmt(  "fx_max_2", 123.456d)
        );

        assertEquals(
                "|123.46|",
                fmt(  "fx_min_2_max_2", 123.456d)
        );

        assertEquals(
                "|123.00|",
                fmt(  "fx_min_2_max_2", 123)
        );

        assertEquals(
                "|123.457|",
                fmt(  "fx_min_0_max_3", 123.456789d)
        );

        assertEquals(
                "|123|",
                fmt(  "fx_min_0_max_3", 123)
        );

        assertEquals(
                "|{NUMBER(): minimumFractionDigits must be <= maximumFractionDigits}|",
                fmt(  "fx_minmax_bad", 123.456d)
        );

    }


    @Test
    public void precisionSigDigits() {
        assertEquals(
                "|123.456|",
                fmt(  "sig_min", 123.456d)
        );

        assertEquals(
                "|123.000|",
                fmt(  "sig_min", 123)
        );

        assertEquals(
                "|12,345.6789|",
                fmt(  "sig_min", 12345.67890d)
        );

        assertEquals(
                "|0.00500000|",
                fmt(  "sig_min", 0.005d)
        );

        assertEquals(
                "|123.4560000|",
                fmt(  "sig_minmax_confusing", 123.456d)
        );

        assertEquals(
                "|123.5|",
                fmt(  "sig_max_4", 123.456789d)
        );

        assertEquals(
                "|123|",
                fmt(  "sig_max_4", 123)
        );

    }


     @Test
    public void skeletons() {
         assertEquals(
                 "|{NUMBER(): Syntax error in skeleton string: Unknown stem: [scary] skeleton }|",
                 fmt(  "skeleton_invalid_skeleton", 123)
         );

         assertEquals(
                 "|{NUMBER(): Too many options; option 'skeleton' can only be used alone, or with 'kind'.}|",
                 fmt(  "skeleton_too_many_options", 123)
         );


         assertEquals(
                 "|5 thousand|",
                 fmt(  "skeleton_one", 5000)
         );


         assertEquals(
                 "|5 thousand|",
                 fmt(  "skeleton_one_concise", 5000)
         );
    }


    @Test
    public void kindIgnoredIfNotInSelector() {
        // converts number to a CLDR plural category (https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html)
        // this is typically used for selectors (and is the default)
        // however 'kind' is ignored if NOT in a selector.

        assertEquals(
                "|200|",
                fmt(  "cardinal_simple", 200)
        );

    }


    @Test
    public void selectCardinalImplicit() {
        assertEquals(
                "You have 0 unread messages.",
                fmt(  "unreadEmails_implicit", 0L)
        );

        assertEquals(
                "You have one unread message.",
                fmt(  "unreadEmails_implicit", 1)
        );

        assertEquals(
                "You have 23 unread messages.",
                fmt(  "unreadEmails_implicit", 23)
        );

        assertEquals(
                "You have 1.23 unread messages.",
                fmt(  "unreadEmails_implicit", 1.23d)
        );
    }


    @Test
    public void selectOrdinal() {
        assertEquals(
                "Take the 1st right.",
                fmt(  "turnRightMessage", 1)
        );

        assertEquals(
                "Take the 2nd right.",
                fmt(  "turnRightMessage", 2)
        );

        assertEquals(
                "Take the 3rd right.",
                fmt(  "turnRightMessage", 3)
        );

        assertEquals(
                "Take the 4th right.",
                fmt(  "turnRightMessage", 4)
        );

        assertEquals(
                "Take the 21st right.",
                fmt(  "turnRightMessage", 21)
        );


        assertEquals(
                "Take the 42nd right.",
                fmt(  "turnRightMessage", 42)
        );

        assertEquals(
                "Take the 53rd right.",
                fmt(  "turnRightMessage", 53)
        );

        assertEquals(
                "Take the 99th right.",
                fmt(  "turnRightMessage", 99)
        );
    }

    @Test
    public void selectExact() {
        assertEquals(
                "Zero.",
                fmt( "exactMessageExample", 0 )
        );

        assertEquals(
                "Two point five.",
                fmt( "exactMessageExample", 2.5d )
        );

        assertEquals(
                "Three.",
                fmt( "exactMessageExample", 3 )
        );

        assertEquals(
                "The value is 1.1.",
                fmt( "exactMessageExample", 1.1 )
        );
    }
}
