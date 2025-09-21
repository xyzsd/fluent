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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemporalFnTest {

    static final String RESOURCE = "functions/temporal_fn.ftl";
    // constants for testing
    static final LocalDateTime DATE_AND_TIME = LocalDateTime.of( 2025, 9, 10, 11, 12, 13 );
    static final LocalDate DATE = LocalDate.of( 2034, 5, 6 );
    static final LocalTime TIME = LocalTime.of( 12, 13, 14 );
    static final ZonedDateTime ZONED_DATE_AND_TIME = ZonedDateTime.of( DATE_AND_TIME, ZoneId.of( "UTC" ) );
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.extendedBundleSetup( resource, false );
    }

    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    @Test
    public void invalid() {
        assertEquals(
                "{TEMPORAL(): Missing required option 'pattern' or 'as'.}",
                FTLTestUtils.fmt( bundle, "missing_required", Map.of( "temporal", DATE_AND_TIME ) )
        );

        assertEquals(
                "{TEMPORAL(): Unknown pattern letter: b}",
                FTLTestUtils.fmt( bundle, "invalid_pattern", Map.of( "temporal", DATE_AND_TIME ) )
        );

        assertEquals(
                """
                        {TEMPORAL(): Named option 'badpredefined': unrecognized. Allowed values: [BASIC_ISO_DATE, \
                        ISO_DATE, ISO_DATE_TIME, ISO_INSTANT, ISO_LOCAL_DATE, ISO_LOCAL_DATE_TIME, ISO_LOCAL_TIME, \
                        ISO_OFFSET_DATE, ISO_OFFSET_DATE_TIME, ISO_OFFSET_TIME, ISO_ORDINAL_DATE, ISO_TIME, \
                        ISO_WEEK_DATE, ISO_ZONED_DATE_TIME, RFC_1123_DATE_TIME]}""",
                FTLTestUtils.fmt( bundle, "invalid_predefined", Map.of( "temporal", DATE_AND_TIME ) )
        );
    }

    @Test
    public void predefined() {
        assertEquals(
                "20250910",
                FTLTestUtils.fmt( bundle, "predefined_basic_iso_date", Map.of( "temporal", DATE_AND_TIME ) )
        );

        assertEquals(
                "20340506",
                FTLTestUtils.fmt( bundle, "predefined_basic_iso_date", Map.of( "temporal", DATE ) )
        );

        assertEquals(
                "11:12:13",
                FTLTestUtils.fmt( bundle, "predefined_iso_time", Map.of( "temporal", DATE_AND_TIME ) )
        );

        assertEquals(
                "12:13:14",
                FTLTestUtils.fmt( bundle, "predefined_iso_time", Map.of( "temporal", TIME ) )
        );

        assertEquals(
                "Wed, 10 Sep 2025 11:12:13 GMT",
                FTLTestUtils.fmt( bundle, "predefined_rfc_1123", Map.of( "temporal", ZONED_DATE_AND_TIME ) )
        );
    }


    @Test
    public void patternDate() {

        assertEquals(
                "2025-09-10",
                FTLTestUtils.fmt( bundle, "pattern_date", Map.of( "temporal", DATE_AND_TIME ) )
        );

        assertEquals(
                "2034-05-06",
                FTLTestUtils.fmt( bundle, "pattern_date", Map.of( "temporal", DATE ) )
        );

        assertEquals(
                "{TEMPORAL(): Unsupported field: YearOfEra}",
                FTLTestUtils.fmt( bundle, "pattern_date", Map.of( "temporal", TIME ) )
        );
    }

    @Test
    public void patternTime() {
        assertEquals(
                "11:12:13.0",
                FTLTestUtils.fmt( bundle, "pattern_time", Map.of( "temporal", DATE_AND_TIME ) )
        );

        // DATE has no time
        assertEquals(
                "{TEMPORAL(): Unsupported field: HourOfDay}",
                FTLTestUtils.fmt( bundle, "pattern_time", Map.of( "temporal", DATE ) )
        );

        assertEquals(
                "12:13:14.0",
                FTLTestUtils.fmt( bundle, "pattern_time", Map.of( "temporal", TIME ) )
        );
    }

    @Test
    public void patternBoth() {
        assertEquals(
                "2025-09-10, 11:12:13.0",
                FTLTestUtils.fmt( bundle, "pattern_both", Map.of( "temporal", DATE_AND_TIME ) )
        );

        assertEquals(
                "{TEMPORAL(): Unsupported field: HourOfDay}",
                FTLTestUtils.fmt( bundle, "pattern_both", Map.of( "temporal", DATE ) )
        );

        assertEquals(
                "{TEMPORAL(): Unsupported field: YearOfEra}",
                FTLTestUtils.fmt( bundle, "pattern_both", Map.of( "temporal", TIME ) )
        );
    }
}
