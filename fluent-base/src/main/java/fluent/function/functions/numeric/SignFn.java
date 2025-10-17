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
import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import fluent.function.ResolvedParameters;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/// SIGN() : Sign of a numeric pattern (and more...)
///
/// Returns a string indicating the sign of a numeric pattern.
///
/// Non-numeric values are passed through unchanged.
/// If no values are present, no values will be returned.
///
/// Specifically:
///
///       - negative values => "negative"
///       - zero => "zero"
///       - positive values => "positive"
///
/// For floating-point types, "zero" will be returned for both positive or negative zero. Additionally:
///
///       - NaN => "NaN"
///       - positive infinity => "positiveInfinity"
///       - negative infinity=> "negativeInfinity"
///
/// This function is not sensitive to Locale or Options.
@NullMarked
public enum SignFn implements FluentFunctionFactory<FluentFunction.Transform>, FluentFunction.Transform {

    SIGN;


    private static String sign(FluentNumber<?> fluentNumber) {
        return switch (fluentNumber) {
            case FluentNumber.FluentDouble fluentDouble -> doubleSign( fluentDouble.value() );
            case FluentNumber.FluentLong fluentLong -> signumToString( Long.signum( fluentLong.value() ) );
            case FluentNumber.FluentBigDecimal(BigDecimal b) -> signumToString( b.signum() );
        };
    }

    private static String doubleSign(double value) {
        if (Double.isNaN( value )) {
            return "NaN";
        } else if (value == Double.POSITIVE_INFINITY) {
            return "positiveInfinity";
        } else if (value == Double.NEGATIVE_INFINITY) {
            return "negativeInfinity";
        } else {
            // Important: JLS 15.20.1:  for "<, <=, >, >=", negative and positive 0 compare equally.
            // Not true for Double.compare() though.
            if (value > 0.0d) {
                return "positive";
            } else if (value < 0.0d) {
                return "negative";
            } else {
                return "zero";
            }
        }
    }

    // signum : either -1, 0, or 1 (as per Math.signum())
    private static String signumToString(int signum) {
        if (signum > 0) {
            return "positive";
        } else if (signum < 0) {
            return "negative";
        } else {
            return "zero";
        }
    }

    @Override
    public FluentFunction.Transform create(Locale locale, Options options) {
        // neither Locale nor Options are relevant.
        return SIGN;
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

        final var biConsumer = FluentFunction.mapFluentNumberOrPassthrough( SignFn::sign );
        return parameters.positionals().mapMulti( biConsumer ).toList();
    }


}
