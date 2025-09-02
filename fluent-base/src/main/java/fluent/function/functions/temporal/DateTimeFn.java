/*
 *
 *  Copyright (C) 2025, xyzsd (Zach Del)
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
package fluent.function.functions.temporal;


import fluent.bundle.resolver.Scope;
import fluent.function.*;
import fluent.syntax.AST.VariantKey;
import fluent.types.FluentString;
import fluent.types.FluentTemporal;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.List;
import java.util.Locale;

/*
https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat
const DATETIME_ALLOWED = [
  "dateStyle",  full,long,medium,short
  "timeStyle",  full,long,medium,short


  "fractionalSecondDigits", can be '1', '2', or '3'
  "dayPeriod",  narrow,short,long
  "hour12",     true,false (true = 23/24 hour time)
  "weekday",    long, short, narrow
  "era",    long, short, narrow
  "year",   numeric, 2-digit
  "month",  numeric, 2-digit, long, short, narrow
  "day",    numeric, 2-digit
  "hour",   numeric, 2-digit
  "minute", numeric, 2-digit
  "second", numeric, 2-digit
  "timeZoneName",   long,short,shortOffset,longOffset,shortGeneric,longGeneric
];

https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat

*** IMPORTANT:
Note: dateStyle and timeStyle can be used with each other, but not with other date-time component options (e.g., weekday, hour, month, etc.).

 */
///      CAVEATS:
///         - Some format options will require a timezone.
///         - this function cannot be used to localize or display `TemporalAmount`s such as `Duration` or `Period`.
///         - `Instants` can be formatted; these are special-cased with `ZoneOffset.UTC`
@NullMarked
public enum DateTimeFn implements FluentFunctionFactory<FluentFunction.Formatter<TemporalAccessor>> {

    DATETIME;


    @Override
    public FluentFunction.Formatter<TemporalAccessor> create(Locale locale, Options options) {
        final FormatStyle timeStyle = options.asEnum( FormatStyle.class, "timeStyle" )
                .orElse( FormatStyle.MEDIUM );

        final FormatStyle dateStyle = options.asEnum( FormatStyle.class, "dateStyle" )
                .orElse( FormatStyle.MEDIUM );

        return new DTF3(locale, timeStyle, dateStyle);
    }

    @Override
    public boolean canCache() {
        return true;
    }


    @NullMarked
    private static class DTF3 implements FluentFunction.Formatter<TemporalAccessor> {
        // StableValue could be useful here
        private final DateTimeFormatter dtf;
        private final DateTimeFormatter df;
        private final DateTimeFormatter tf;

        public DTF3(Locale locale, FormatStyle timeStyle, FormatStyle dateStyle) {
            dtf = DateTimeFormatter.ofLocalizedDateTime( dateStyle, timeStyle ).withLocale( locale );
            df = DateTimeFormatter.ofLocalizedDate( dateStyle ).withLocale( locale );
            tf = DateTimeFormatter.ofLocalizedTime( timeStyle ).withLocale( locale );
        }

        @Override
        public FluentValue<String> format(FluentValue<? extends TemporalAccessor> in, Scope scope) {
            return FluentString.of( formatToString( in.value() ) );
        }



        @Override
        public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
            FluentFunction.ensureInput( parameters );

            final var biConsumer = FluentFunction.mapOrPassthrough( TemporalAccessor.class, this::formatToString );
            return parameters.positionals()
                    .mapMulti( biConsumer )
                    .toList();
        }


        private String formatToString(TemporalAccessor temporalAccessor) {
            // special case for Instants, which fail the localDate and localTime queries
            if (temporalAccessor instanceof Instant instant) {
                temporalAccessor = LocalDateTime.ofInstant( instant, ZoneOffset.UTC );
            }

            final boolean hasDate = (temporalAccessor.query( TemporalQueries.localDate() ) != null);
            final boolean hasTime = (temporalAccessor.query( TemporalQueries.localTime() ) != null);

            final DateTimeFormatter selectedFormatter;
            if (hasDate && hasTime) {
                selectedFormatter = dtf;
            } else if (hasDate) {
                selectedFormatter = df;
            } else if (hasTime) {
                selectedFormatter = tf;
            } else {
                throw FluentFunctionException.of(
                        "No local date OR time for temporal: '%s'",
                        temporalAccessor
                );
            }

            try {
                return selectedFormatter.format( temporalAccessor );
            } catch (DateTimeException e) {
                throw FluentFunctionException.of( e );
            }
        }

        @Override
        public VariantKey select(ResolvedParameters parameters, List<VariantKey> variantKeys, VariantKey defaultKey, Scope scope) {
            final FluentValue<?> fluentValue = Selector.ensureSingle( parameters );
            if (fluentValue instanceof FluentTemporal(TemporalAccessor value)) {
                return VariantKey.matchKey(  formatToString( value ), variantKeys, defaultKey);
            } else {
                return defaultKey;
            }
        }
    }
}
