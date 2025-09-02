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

@NullMarked
public enum ExtractTemporalFn implements FluentFunctionFactory<FluentFunction.Transform>  {


    /*
    used to extract a date or time pattern
    this is a transformation
    preserves type
    e.g., if 'day of month' extracted for december, pattern would be '12' (as a FluentNumber) not "12" (as a FluentString)


    supports all fields in ChronoField

    temporalAccess.getLong(TemporalField)
    may return UnsupportedTemporalTypeException (error)
     */

        XTEMPORAL;


        @Override
        public FluentFunction.Transform create(final Locale locale, final Options options) {
            final ChronoField field = options.asEnum( ChronoField.class, "field" ).orElseThrow( () ->
                    FluentFunctionException.of( "Option 'field' is required!" ) );

            return FluentFunction.passthroughTransform(TemporalAccessor.class, extractorFor(field) );
        }

        @Override
        public boolean canCache() {
            return true;
        }

        // create the extractor function
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
