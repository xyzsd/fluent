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
import java.time.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeFnTest {


    static final String RESOURCE = "functions/datetime_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    // constants for testing
    static final LocalDateTime DATE_AND_TIME = LocalDateTime.of(2025,9,10,11, 12, 13);
    static final LocalDate DATE = LocalDate.of(2034, 5, 6);
    static final LocalTime TIME = LocalTime.of(12,13,14);

    static final ZonedDateTime ZONED_DATE_AND_TIME = ZonedDateTime.of(DATE_AND_TIME, ZoneId.of("UTC") );


    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }



    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }

    @Test
    public void implicit() {
        assertEquals(
                "|Sep 10, 2025, 11:12:13 AM|", // NOTE! there is a non-breaking space (NBSP) between the time and the am/pm indicator
                FTLTestUtils.fmt( bundle, "dt_default", Map.of("temporal", DATE_AND_TIME) )
        );

        assertEquals(
                "|May 6, 2034|",
                FTLTestUtils.fmt( bundle, "dt_default", Map.of("temporal", DATE) )
        );

        assertEquals(
                "|12:13:14 PM|",    // NOTE! there is a non-breaking space (NBSP) between the time and the am/pm indicator
                FTLTestUtils.fmt( bundle, "dt_default", Map.of("temporal", TIME) )
        );


    }

    @Test
    public void instant() {
        // Instants have a default zone of UTC. These can be formatted, as the DATETIME formatter converts
        // them into "ZonedDateTime".
        assertEquals(
                "|Jan 1, 1970, 12:00:00 AM|",
                FTLTestUtils.fmt( bundle, "dt_default", Map.of("temporal", Instant.EPOCH) )
        );
    }

    @Test
    public void invalidArgument() {
        // passes through
        assertEquals(
                "|just a string|",
                FTLTestUtils.fmt( bundle, "dt_full_full", Map.of( "temporal", "just a string" ) )
        );

    }

    @Test
    public void fullFull() {
        assertEquals(
                "|Wednesday, September 10, 2025, 11:12:13 AM Coordinated Universal Time|",
                FTLTestUtils.fmt( bundle, "dt_full_full", Map.of("temporal", ZONED_DATE_AND_TIME) )
        );

        // 'full' requires a zone!
        assertEquals(
                "|{DATETIME(): Unable to extract ZoneId from temporal 2025-09-10T11:12:13}|",
                FTLTestUtils.fmt( bundle, "dt_full_full", Map.of("temporal", DATE_AND_TIME) )
        );

        assertEquals(
                "|Saturday, May 6, 2034|",
                FTLTestUtils.fmt( bundle, "dt_full_full", Map.of("temporal", DATE) )
        );

        assertEquals(
                "|{DATETIME(): Unable to extract ZoneId from temporal 12:13:14 with chronology ISO}|",
                FTLTestUtils.fmt( bundle, "dt_full_full", Map.of("temporal", TIME) )
        );
    }

    @Test
    public void shortShort() {
        assertEquals(
                "|9/10/25, 11:12 AM|",
                FTLTestUtils.fmt( bundle, "dt_short_short", Map.of("temporal", ZONED_DATE_AND_TIME) )
        );

        // 'short' does NOT require a zone!
        assertEquals(
                "|9/10/25, 11:12 AM|",
                FTLTestUtils.fmt( bundle, "dt_short_short", Map.of("temporal", DATE_AND_TIME) )
        );

        assertEquals(
                "|5/6/34|",
                FTLTestUtils.fmt( bundle, "dt_short_short", Map.of("temporal", DATE) )
        );

        assertEquals(
                "|12:13 PM|",
                FTLTestUtils.fmt( bundle, "dt_short_short", Map.of("temporal", TIME) )
        );
    }

    @Test
    public void mediumMedium() {
        assertEquals(
                "|Sep 10, 2025, 11:12:13 AM|",
                FTLTestUtils.fmt( bundle, "dt_medium_medium", Map.of("temporal", ZONED_DATE_AND_TIME) )
        );

        // 'medium' does NOT require a zone!
        assertEquals(
                "|Sep 10, 2025, 11:12:13 AM|",
                FTLTestUtils.fmt( bundle, "dt_medium_medium", Map.of("temporal", DATE_AND_TIME) )
        );

        assertEquals(
                "|May 6, 2034|",
                FTLTestUtils.fmt( bundle, "dt_medium_medium", Map.of("temporal", DATE) )
        );

        assertEquals(
                "|12:13:14 PM|",
                FTLTestUtils.fmt( bundle, "dt_medium_medium", Map.of("temporal", TIME) )
        );
    }

    @Test
    public void longLong() {
        assertEquals(
                "|September 10, 2025, 11:12:13 AM UTC|",
                FTLTestUtils.fmt( bundle, "dt_long_long", Map.of("temporal", ZONED_DATE_AND_TIME) )
        );

        // 'long' requires a zone!
        assertEquals(
                "|{DATETIME(): Unable to extract ZoneId from temporal 2025-09-10T11:12:13}|",
                FTLTestUtils.fmt( bundle, "dt_long_long", Map.of("temporal", DATE_AND_TIME) )
        );

        assertEquals(
                "|May 6, 2034|",
                FTLTestUtils.fmt( bundle, "dt_long_long", Map.of("temporal", DATE) )
        );

        // zone required for 'long' style
        assertEquals(
                "|{DATETIME(): Unable to extract ZoneId from temporal 12:13:14 with chronology ISO}|",
                FTLTestUtils.fmt( bundle, "dt_long_long", Map.of("temporal", TIME) )
        );
    }


}
