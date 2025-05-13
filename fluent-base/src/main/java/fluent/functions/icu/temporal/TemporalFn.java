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

package fluent.functions.icu.temporal;

import fluent.bundle.resolver.Scope;
import fluent.functions.*;
import fluent.types.FluentString;
import fluent.types.FluentTemporal;
import fluent.types.FluentValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.List;
import java.util.Locale;

/**
 *  TEMPORAL() : The (implicit) formatter of time values
 *  <p>
 *      At least one value is required. Non-temporal values are passed through unchanged.
 *  </p>
 *  <p>
 *      Uses the default formatter for the locale
 *  </p>
 *  <p>
 *      Options:
 *          <ul>
 *              <li>{@code dateStyle:} "full", "long", "medium", "short"</li>
 *              <li>{@code timeStyle:} "full", "long", "medium", "short"</li>
 *              <li>
 *                  {@code pattern:} String pattern per DateTimeFormatter (e.g., "hh:mm").
 *                  This option will override dateStyle and/or timeStyle if present.
 *              </li>
 *              <li>{@code timeZone:} (optional) time zone (java.time.ZoneId.getID())</li>
 *          </ul>
 *  <p>
 *      CAVEATS:
 *      <ul>
 *          <li>Some format options will require a timezone.</li>
 *          <li>
 *              this function cannot be used to localize or display {@code TemporalAmount}s such as
 *              {@code Duration} or {@code Period}.
 *          </li>
 *      </ul>
 *
 */
public class TemporalFn implements FluentImplicit, ImplicitFormatter {

    // potential improvements:
    //      consider: adding locale/chronology specification support
    //      consider: depending upon performance, could keep a cache of CustomDTF formatters based on style/pattern
    //      consider: caching global options


    public TemporalFn() {}

    @Override
    public Implicit id() {
        return Implicit.TEMPORAL;
    }

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters params, final Scope scope) {
        FluentFunction.ensureInput( params );

        CustomDTF dtf = new CustomDTF(params.options(), scope.bundle().locale() );

        return params.valuesAll()
                .<FluentValue<?>>map( dtf::format )
                .toList();
    }

    // this will fail if 'in' is not a FluentTemporal!
    @Override
    public String format(FluentValue<?> in, Scope scope) {
        final CustomDTF dtf = new CustomDTF( scope.options(), scope.bundle().locale() );
        return dtf.format(in).format( scope );
    }


    // this class must be immutable if we want to cache it
    private static class CustomDTF {
        @NotNull private final Locale locale;
        @Nullable private final FormatStyle dateStyle;  // only null if dtf is NOT null
        @Nullable private final FormatStyle timeStyle;  // only null if dtf is NOT null
        @Nullable private final ZoneId tz;  // override zone for DateTimeFormatter; null is OK
        @Nullable private final DateTimeFormatter dtf;    // null unless pattern-based


        CustomDTF(final Options opts, final Locale locale) {
            this.locale = locale;
            this.tz = opts.into("timeZone", ZoneId::of).orElse( null );

            if (opts.has( "pattern" )) {
                // 'pattern' format has primacy
                dtf = opts.into("pattern", CustomDTF::parseDTFPattern)
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
        FluentValue<?> format(final FluentValue<?> in) {
            if (in instanceof FluentTemporal temporal) {
                TemporalAccessor temporalAccessor = temporal.value();

                // special case for Instants, which fail the localDate and localTime queries
                if(temporalAccessor instanceof Instant instant) {
                    temporalAccessor = LocalDateTime.ofInstant( instant, ZoneOffset.UTC );
                }

                DateTimeFormatter formatter = dtf;
                if(dtf == null) {
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
                        throw FluentFunctionException.create(
                                "No local date OR time for temporal: '%s'",
                                temporalAccessor
                        );
                    }

                    formatter = formatter.withZone( tz );
                }

                assert (formatter != null);

                try {
                    return FluentString.of( formatter.format( temporalAccessor ) );
                } catch(DateTimeException e) {
                    throw FluentFunctionException.wrap( e );
                }
            }

            return in; // pass-through
        }
    }

}
