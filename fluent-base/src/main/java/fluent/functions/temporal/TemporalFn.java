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

package fluent.functions.temporal;

import fluent.bundle.resolver.Scope;
import fluent.functions.*;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.List;
import java.util.Locale;

///  TEMPORAL(): The (implicit) formatter of time values
///
///      At least one value is required. Non-temporal values are passed through unchanged.
///
///      Uses the default formatter for the locale
///
///      Options:
///            - `dateStyle:` "full", "long", "medium", "short"
///            - `timeStyle:` "full", "long", "medium", "short"
///            - `pattern:` String pattern per DateTimeFormatter (e.g., "hh:mm").
///     This option will override dateStyle and/or timeStyle if present.
///            - `timeZone:` (optional) time zone (java.time.ZoneId.getID())
///
///      CAVEATS:
///        - Some format options will require a timezone.
///        - this function cannot be used to localize or display `TemporalAmount`s such as `Duration` or `Period`.
///
///
@NullMarked
public enum TemporalFn implements FluentFunction, ImplicitFormatter<TemporalAccessor> {

    // potential improvements:
    //      consider: adding locale/chronology specification support
    //      consider: depending upon performance, could keep a cache of CustomDTF formatters based on style/pattern
    //      consider: caching global options
    //      consider: CLDR semantic skeletons
    // also re-look at implementing DATE, TIME, and DATETIME

    TEMPORAL;

/*
    @Override
    public String name() {
        return "TEMPORAL";
    }
*/
    @Override
    public String format(FluentValue<? extends TemporalAccessor> in, Scope scope) {
        final CustomDTF dtf = new CustomDTF( scope.options(), scope.bundle().locale() );
        return  dtf.format( in.value()  );
    }

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        FluentFunction.ensureInput( parameters );

        CustomDTF dtf = new CustomDTF(parameters.options(), scope.bundle().locale() );

        var biConsumer = FluentFunction.mapOrPassthrough( TemporalAccessor.class, dtf::format );
        return parameters.positionals().mapMulti( biConsumer ).toList();
    }



    // this class must be immutable if we want to cache it (todo: and verify thread safety)
    private static class CustomDTF {
        private final Locale locale;
        @Nullable private final FormatStyle dateStyle;  // only null if dtf is NOT null
        @Nullable private final FormatStyle timeStyle;  // only null if dtf is NOT null
        @Nullable private final ZoneId tz;  // override zone for DateTimeFormatter; null is OK
        @Nullable private final DateTimeFormatter dtf;    // null unless pattern-based


        CustomDTF(final Options opts, final Locale locale) {
            this.locale = locale;
            this.tz = opts.into( "timeZone", ZoneId::of ).orElse( null );

            if (opts.has( "pattern" )) {
                // 'pattern' format has primacy
                dtf = opts.into( "pattern", CustomDTF::parseDTFPattern )
                        .orElseThrow()
                        .withLocale( locale )
                        .withZone( tz );

                timeStyle = null;
                dateStyle = null;
            } else {
                // time/date style format
                timeStyle = opts.asEnum( FormatStyle.class, "timeStyle" )
                        .orElse( FormatStyle.MEDIUM );

                dateStyle = opts.asEnum( FormatStyle.class, "dateStyle" )
                        .orElse( FormatStyle.MEDIUM );

                this.dtf = null;
            }
        }

        private static DateTimeFormatter parseDTFPattern(final String in) {
            try {
                return DateTimeFormatter.ofPattern( in );
            } catch (IllegalArgumentException e) {
                throw FluentFunctionException.wrap( e );
            }
        }

        // pass-through non-temporals
        String format(final TemporalAccessor in) {
            TemporalAccessor temporalAccessor = in;

            // special case for Instants, which fail the localDate and localTime queries
            if (temporalAccessor instanceof Instant instant) {
                temporalAccessor = LocalDateTime.ofInstant( instant, ZoneOffset.UTC );
            }

            DateTimeFormatter formatter = dtf;
            if (dtf == null) {
                final boolean hasDate = (temporalAccessor.query( TemporalQueries.localDate() ) != null);
                final boolean hasTime = (temporalAccessor.query( TemporalQueries.localTime() ) != null);

                if (hasDate && hasTime) {
                    formatter = DateTimeFormatter.ofLocalizedDateTime( dateStyle, timeStyle )
                            .withLocale( locale );
                } else if (hasDate) {
                    formatter = DateTimeFormatter.ofLocalizedDate( dateStyle )
                            .withLocale( locale );
                } else if (hasTime) {
                    formatter = DateTimeFormatter.ofLocalizedTime( timeStyle )
                            .withLocale( locale );
                } else {
                    throw FluentFunctionException.of(
                            "No local date OR time for temporal: '%s'",
                            temporalAccessor
                    );
                }

                formatter = formatter.withZone( tz );
            }

            assert (formatter != null);

            try {
                return formatter.format( temporalAccessor );
            } catch (DateTimeException e) {
                throw FluentFunctionException.wrap( e );
            }
        }
    }

}
