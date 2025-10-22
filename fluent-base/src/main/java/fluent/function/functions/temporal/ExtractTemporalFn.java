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

import fluent.function.*;
import org.jspecify.annotations.NullMarked;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.function.Function;

/// ## XTEMPORAL()
/// Extract a single temporal field from a date/time value.
///
/// Transformation function that reads one of the [ChronoField] fields from an input
/// [TemporalAccessor] (e.g., [java.time.LocalDate], [java.time.Instant],
/// [java.time.ZonedDateTime], etc.) and replaces that item with the extracted numeric value.
/// Non-temporal inputs are passed through unchanged. Errors are passed through unchanged.
///
/// ## Options
///     - **field** (required): The [ChronoField] to extract. Any constant from `ChronoField`
///     is accepted, for example: `YEAR`, `MONTH_OF_YEAR`, `MINUTE_OF_HOUR`, etc.
///
/// ## Behavior
///     - If the input is a Fluent temporal value, the value for the requested field
///     is returend as a [fluent.types.FluentNumber].
///     - If the input is not a temporal value, it is left unchanged (passthrough).
///     - If the field is not supported by the input temporal type, or the value cannot be represented as a
///     `long`, an exception is raised and wrapped as a [FluentFunctionException].
///     - This function does not perform timezone conversions; it simply delegates to the underlying temporal
///     object's field value. For instant-like inputs, the field semantics are those of the temporal type you pass
///     (e.g., extracting `YEAR` from an [java.time.Instant] is not supported, but extracting
///     `INSTANT_SECONDS` is).
///
/// ## Examples
/// {@snippet :
///   # FTL examples
///   # Given: $birthday = 2000-05-10 (a LocalDate)
///   { XTEMPORAL($birthday, field:"DAY_OF_MONTH") }   # -> 10
///   { XTEMPORAL($birthday, field:"MONTH_OF_YEAR") }  # -> 5
///
///   # Using with selection
///   # Choose a message by weekday number (1-7) from a LocalDate
///   { $date ->
///       [1] Monday
///       [2] Tuesday
///       [3] Wednesday
///       [4] Thursday
///       [5] Friday
///       [6] Saturday
///       [7] Sunday
///      *[other] Unknown
///   }
///   # You can first extract the day of week, then select on the result:
///   { SELECT(XTEMPORAL($date, field:"DAY_OF_WEEK")) }
/// }
///
///
@NullMarked
public enum ExtractTemporalFn implements FluentFunctionFactory<FluentFunction.Transform>  {

    /// Single enum constant representing the function name in FTL.
    XTEMPORAL;


    /// {@inheritDoc}
    @Override
    public FluentFunction.Transform create(final Locale locale, final Options options) {
        final ChronoField field = options.asEnum( ChronoField.class, "field" ).orElseThrow( () ->
                FluentFunctionException.of( "Option 'field' is required!" ) );

        return FluentFunction.passthroughTransform(TemporalAccessor.class, extractorFor(field) );
    }

    /// The transform is cacheable because it is immutable and stateless once constructed.
    @Override
    public boolean canCache() {
        return true;
    }

    /// create the extractor
    private static Function<TemporalAccessor,Long> extractorFor(final ChronoField field) {
        return (ta) -> {
            try {
                return ta.getLong( field );
            } catch(DateTimeException | ArithmeticException e) {
                throw FluentFunctionException.of(e);
            }
        };
    }

}
