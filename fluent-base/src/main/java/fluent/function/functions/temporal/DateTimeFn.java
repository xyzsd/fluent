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

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.*;
import java.util.List;
import java.util.Locale;

/// DATETIME(): basic date and time formatting
///
/// This does not replace or attempt to implement all the functions of JavaScript Intl.DateFormat
///
/// Only two options are supported:
/// - `dateStyle` : full,long,medium,short
/// - `timeStyle` : full,long,medium,short
///
/// For more powerful formatting of dates and times, use the TEMPORAL function.
///
/// Alternatively, the DATETIME implicit can be replaced with TEMPORAL if desired.
/// {@snippet :
///    // TODO: illustrate how to replace DATETIME with TEMPORAL
///
/// }
///
/// IMPORTANT NOTES:
/// - Some format options (e.g., timeStyle:FULL or LONG) will require a timezone.
/// - LocalDateTime, LocalTime do NOT have time zones defined
/// - this function cannot be used to localize or display [TemporalAmount][java.time.temporal.TemporalAmount]s
///             such as `Duration` or `Period`.
/// - Special handling for [Instant][java.time.Instant]: Instants are converted to ZonedDateTime (zone: UTC),
///             so that they can be formatted (because Instants are created with the UTC time zone)
///
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


        private String formatToString(final TemporalAccessor in) {
            final TemporalAccessor temporalAccessor;
            if(in instanceof Instant instant) {
                temporalAccessor = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            } else {
                temporalAccessor = in;
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
                        "No date or time for temporal: '%s'",
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
