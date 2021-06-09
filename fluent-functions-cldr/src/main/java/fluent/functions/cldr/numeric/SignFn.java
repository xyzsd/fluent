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

package fluent.functions.cldr.numeric;

import fluent.functions.FluentFunction;
import fluent.functions.ResolvedParameters;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentString;
import fluent.types.FluentValue;

import java.util.List;

/**
 * SIGN() : Sign of a numeric value (and more..)
 * <p>
 * Returns a string indicating the sign of a numeric value.
 * Non-numeric values are passed through. At least one value is required.
 * </p>
 * <p>
 * Specifically:
 *     <ul>
 *         <li>negative values => "negative"</li>
 *         <li>zero => "zero"</li>
 *         <li>positive values => "positive"</li>
 *     </ul>
 * For floating-point types, "zero" will be returned for positive or negative zero. Additionally:
 * <ul>
 *         <li>NaN => "nan"</li>
 *         <li>positive infinity => "positiveInfinity"</li>
 *         <li>negative infinity=> "negativeInfinity"</li>
 *    </ul>
 */
public class SignFn implements FluentFunction {

    public static final String NAME = "SIGN";

    public SignFn() {}


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<FluentValue<?>> apply(ResolvedParameters params, Scope scope) {
        FluentFunction.ensureInput( params );    // SIGN() [no arguments]: not legal

        return FluentFunction.mapOverNumbers( params.valuesAll(),
                scope, SignFn::sign );
    }

    private static FluentValue<?> sign(Number number) {
        if (number instanceof Double d) {
            if (Double.isNaN( d )) {
                return new FluentString( "nan" );
            } else if (d == Double.POSITIVE_INFINITY) {
                return new FluentString( "positiveInfinity" );
            } else if (d == Double.NEGATIVE_INFINITY) {
                return new FluentString( "negativeInfinity" );
            }
        }

        long l = number.longValue();

        if (l == 0) {
            return new FluentString( "zero" );
        } else {
            return new FluentString( (l > 0) ? "positive" : "negative" );
        }
    }
}
