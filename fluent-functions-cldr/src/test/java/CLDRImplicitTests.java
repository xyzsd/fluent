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

import fluent.functions.cldr.CLDRFunctionFactory;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CLDRImplicitTests {

    static final CommonTest t = CommonTest.init(
            CLDRFunctionFactory.INSTANCE,
            Locale.US,
            true
    );


    // constants for use in tests

    static final List<String> ALPHA_1 = List.of(
            "alpha"
    );

    static final List<String> ALPHA_2 = List.of(
            "alpha","beta"
    );

    static final List<String> ALPHA_3 = List.of(
            "alpha","beta","gamma"
    );

    static final List<String> ALPHA_6 = List.of(
            "alpha","beta","gamma","delta","epsilon","zeta"
    );

    // check that implicit and explicit forms are equivalent
    // then check arrays (JOIN)
    // then check some options


    // JOIN
    @Test
    void joinEqv() {
        String implicit = t.msg(
                "hello = Hello there, {$name}!\n",
                "hello",
                Map.of("name", "User#1") );
        assertEquals("Hello there, User#1!", implicit);

        String explicit = t.msg(
                "hello = Hello there, {JOIN($name)}!\n",
                "hello",
                Map.of("name", "User#1") );
        assertEquals("Hello there, User#1!", explicit);

        assertEquals(implicit, explicit);
    }

    // NUMBER
    @Test
    void numberEqv() {
        String implicit = t.msg(
                "hello = Hello there, user {$idnum}!\n",
                "hello",
                Map.of("idnum", 999) );
        assertEquals("Hello there, user 999!", implicit);

        String explicit = t.msg(
                "hello = Hello there, user {NUMBER($idnum)}!\n",
                "hello",
                Map.of("idnum", 999) );
        assertEquals("Hello there, user 999!", explicit);

        assertEquals(implicit, explicit);
    }

    // TEMPORAL ... this test is far from comprehensive
    @Test
    void temporalTests() {
        final LocalDate date = LocalDate.of(2021, Month.JANUARY, 31);
        final LocalTime time = LocalTime.of(1, 23, 45);
        final LocalDateTime dateTime = LocalDateTime.of(date, time);

        // equivalence
        final String implicit = t.msg("msg = {$date}\n", "msg", Map.of("date", dateTime) );
        assertEquals("Jan 31, 2021, 1:23:45 AM", implicit);

        final String explicit = t.msg("msg = {TEMPORAL($date)}\n", "msg", Map.of("date", dateTime) );
        assertEquals("Jan 31, 2021, 1:23:45 AM", implicit);

        assertEquals(implicit, explicit);

        // forms ...
        assertEquals(
                "Sunday, January 31, 2021",
                t.msg("msg = {TEMPORAL($date,dateStyle:\"full\")}\n",
                        "msg",
                        Map.of("date", date))
        );

        // pattern
        assertEquals(
                "01:23",
                t.msg("msg = {TEMPORAL($date,pattern:\"hh:mm\")}\n",
                        "msg",
                        Map.of("date", dateTime))
        );

        // this pattern is illegal for date (time pattern)
        // and an exception will occur. make sure the exception is appropriately handled.
        assertEquals(
                "{TEMPORAL()}",
                t.msg("msg = {TEMPORAL($date,pattern:\"hh:mm\")}\n",
                        "msg",
                        Map.of("date", date))
        );

        // 'instant'
        // note: currently, instants are UTC-based as we have no means (yet!)
        //       to specify the timezone
        //
        // 1609556645L: 2021 January 2, 3:04:05
        final Instant instant = Instant.ofEpochSecond( 1609556645L );
        assertEquals(
                "Jan 2, 2021, 3:04:05 AM",
                t.msg("msg = {TEMPORAL($instant)}\n",
                        "msg",
                        Map.of("instant", instant))
        );
    }


    // JOIN with lists
    @Test
    void simpleListTest() {
        final Map<String,?> map = Map.of("name", ALPHA_3);

        // implicit
        String ftl = "hello = Hello: {$name}!\n";
        assertEquals(
                "Hello: alpha, beta, gamma!",
                t.msg( ftl, "hello", map )
        );

        // explicit
        ftl = "hello = Hello: {JOIN($name)}!\n";
        assertEquals(
                "Hello: alpha, beta, gamma!",
                t.msg( ftl, "hello", map )
        );

        // junction and pair
        // here we use 'and', but 'or' could be used if appropriate
        // note:
        //      junction:", and "   // use a serial comma (a.k.a. 'Harvard' or 'Oxford' comma)
        //              A, B, and C
        //      junction:" and "   // omit serial comma
        //              A, B and C
        //      junction:", "      // just use a comma + space
        //              A, B, C
        //
        ftl = """
            hello = Hello: {JOIN($name, junction:", and ", pairSeparator:" and ")}!
            """;

        assertEquals(
                "Hello: alpha, beta, and gamma!",
                t.msg( ftl, "hello", map )
        );

        // now test pair, with a pair..
        final Map<String,?> pair = Map.of("name", ALPHA_2);

        assertEquals(
                "Hello: alpha and beta!",
                t.msg( ftl, "hello", pair )
        );
    }



    // NUMBER formatting
    @Test
    void numberFnTest() {
        assertEquals(
                "NaN",
                t.msg( "msg = {$num}", "msg", Map.of("num", Double.NaN) )
        );

        // note: requires UTF-8 source encoding specified in gradle build for this to work
        assertEquals(
                "-∞",
                t.msg( "msg = {$num}", "msg", Map.of("num", Double.NEGATIVE_INFINITY) )
        );

        assertEquals(
                "∞",
                t.msg( "msg = {$num}", "msg", Map.of("num", Double.POSITIVE_INFINITY) )
        );


        String s1 = """
                n10 = { NUMBER() }
                n15 = { NUMBER("notanumber") }
                n20 = { NUMBER(2345) }
                n30 = { NUMBER(2345.1888888) }
                n40 = { NUMBER(2345.1888888, 999.99999) }
                n50 = { NUMBER(12345.56789, useGrouping:"false") }
                n60 = { NUMBER(12345.56789, maximumFractionDigits:1) }
                n70 = { NUMBER(0.99, style:"percent") }
                """;

        // Locale.US assumed for all of the following

        assertEquals(
                "{NUMBER()}",   // an argument is required
                t.msg( s1, "n10"  )
        );
        assertEquals(
                "notanumber",   // non-numeric values are passed through
                t.msg( s1, "n15"  )
        );
        assertEquals(
                "2,345",
                t.msg( s1, "n20"  )
        );
        assertEquals(
                "2,345.189",
                t.msg( s1, "n30"  )
        );
        assertEquals(
                "2,345.189, 1,000",
                t.msg( s1, "n40"  )
        );
        assertEquals(
                "12345.568",
                t.msg( s1, "n50"  )
        );
        assertEquals(
                "12,345.6",
                t.msg( s1, "n60"  )
        );
        assertEquals(
                "99%",
                t.msg( s1, "n70"  )
        );

        // significant digits tests
        s1 = """
                s10 = { NUMBER(123456789.987654321, minimumSignificantDigits:2, maximumSignificantDigits:1) }                
                s20 = { NUMBER(123456789.987654321, minimumSignificantDigits:3, maximumSignificantDigits:20) }                
                s30 = { NUMBER(123456789.987654321, maximumSignificantDigits:5) }                
                s40 = { NUMBER(12345.1, minimumSignificantDigits:9, maximumSignificantDigits:20) }                
                s50 = { NUMBER(12345,   minimumSignificantDigits:9, maximumSignificantDigits:20) }                
                """;

        // assuming Locale.US
        // decimal formatting for this locale is (default) 3 places

        // min > max; this is an error
        assertEquals(
                "{NUMBER()}",
                t.msg( s1, "s10"  )
        );

        assertEquals(
                "123,456,789.988",
                t.msg( s1, "s20"  )
        );

        assertEquals(
                "123,460,000",
                t.msg( s1, "s30"  )
        );

        assertEquals(
                "12,345.10000",
                t.msg( s1, "s40"  )
        );

        // integers are handled differently w.r.t. significant digits...
        // todo: determine if this is desirable
        assertEquals(
                "12,345",
                t.msg( s1, "s50"  )
        );
    }

}
