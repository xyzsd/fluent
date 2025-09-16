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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;

///  TEMPORAL(): Pattern-based formatter of time values
///
///     At least one pattern is required. Non-temporal values are passed through unchanged.
///
///     Required named parameters:
///            - `pattern:` String pattern per DateTimeFormatter (e.g., "hh:mm").
///
///     Optional parameters:
///            - `zone:` (optional) time zone.
///               This option will override dateStyle and/or timeStyle if present.
///
///      CAVEATS:
///        - Some format options will require a timezone.
///        - this function cannot be used to localize or display `TemporalAmount`s such as `Duration` or `Period`.
///
///
@NullMarked
public enum TemporalFn implements FluentFunctionFactory<FluentFunction.Formatter<TemporalAccessor>> {

    TEMPORAL;


    ///  if a 'zone' option is present, add that zone to the given DateTimeFormatter.
    ///  throw an exception if the zone is invalid
    static DateTimeFormatter parseZone(final DateTimeFormatter formatter, final Options options) {
        try {
            ZoneId zone = options.into( "zone", ZoneId::of ).orElse( null );
            return (zone == null) ? formatter : formatter.withZone( zone );
        } catch (DateTimeException e) {
            // invalid / missing zone
            throw FluentFunctionException.of( e );
        }
    }

    @Override
    public FluentFunction.Formatter<TemporalAccessor> create(final Locale locale, final Options options) {
        // required
        final String pattern = options.asString( "pattern" ).orElseThrow(
                () -> FluentFunctionException.of( "Missing required option: 'pattern'" ) );

        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern( pattern ).withLocale( locale );
        } catch (IllegalArgumentException e) {
            // invalid pattern
            throw FluentFunctionException.of( e );
        }

        formatter = parseZone( formatter, options );

        return new DTF( formatter );
    }

    @Override
    public boolean canCache() {
        return true;
    }

    @NullMarked
    static class DTF implements FluentFunction.Formatter<TemporalAccessor> {

        private final DateTimeFormatter formatter;

        DTF(final DateTimeFormatter formatter) {
            this.formatter = formatter;
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

        ///  Select on date/time values should be carefully considered.
        ///  It could potentially be useful, for example, if selection was to occur on a weekday or day-of-week.
        @Override
        public VariantKey select(ResolvedParameters parameters, List<VariantKey> variantKeys, VariantKey defaultKey, Scope scope) {
            final FluentValue<?> fluentValue = Selector.ensureSingle( parameters );
            if (fluentValue instanceof FluentTemporal(TemporalAccessor value)) {
                return VariantKey.matchKey(  formatToString( value ), variantKeys, defaultKey);
            } else {
                return defaultKey;
            }
        }

        private String formatToString(TemporalAccessor temporalAccessor) {
            // special case for Instants, which fail the localDate and localTime queries
            if (temporalAccessor instanceof Instant instant) {
                temporalAccessor = LocalDateTime.ofInstant( instant, ZoneOffset.UTC );
            }

            try {
                return formatter.format( temporalAccessor );
            } catch (DateTimeException e) {
                throw FluentFunctionException.of( e );
            }
        }
    }


}
