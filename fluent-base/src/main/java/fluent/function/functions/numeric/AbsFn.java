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
package fluent.function.functions.numeric;

import fluent.bundle.resolver.Scope;
import fluent.function.*;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

///  ABS() : Absolute pattern
///
///      Returns the absolute pattern of a number, keeping the same numeric type.
///
///      Non-numeric values are passed through.
///      If no values are input, none are returned.
///
///      Examples:
///
///        - ABS(-5) => 5
///        - ABS("string_value") => "string_value"
///
///  This function is not sensitive to Locale or Options.
@NullMarked
public enum AbsFn implements FluentFunctionFactory<FluentFunction.Transform>, FluentFunction.Transform {

    ABS;


    @Override
    public FluentFunction.Transform create(Locale locale, Options options) {
        // neither Locale nor Options are relevant.
        return ABS;
    }

    ///  Not locale or option sensitive; no need to cache.
    @Override
    public boolean canCache() {
        return false;
    }



    @Override
    public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
        if (parameters.isEmpty()) {
            return List.of();
        }

        final var biConsumer = FluentFunction.mapFluentNumberOrPassthrough(  AbsFn::abs );
        return parameters.positionals().mapMulti( biConsumer ).toList();
    }


    private static FluentValue<?> abs(FluentNumber<?> fluentNumber) {
        return switch (fluentNumber) {
            case FluentNumber.FluentLong(Long l) -> new FluentNumber.FluentLong( Math.abs(l) );
            case FluentNumber.FluentDouble(Double d)  -> new FluentNumber.FluentDouble( Math.abs(d) );
            case FluentNumber.FluentBigDecimal(BigDecimal b)  -> new FluentNumber.FluentBigDecimal( b.abs() );
        };
    }

}
