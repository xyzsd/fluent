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

package fluent.functions.numeric;

import fluent.functions.*;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.List;

/**
 *  ABS() : Absolute value
 *  <p>
 *      Returns the absolute value of a number, keeping the same numeric type.
 *  </p>
 *  <p>
 *      Non-numeric values are passed through. At least one value is required.
 *  </p>
 *  <p>
 *      Examples:
 *      <ul>
 *          <li>ABS(-5) => 5</li>
 *          <li>ABS("stringvalue") => "stringvalue" </li>
 *      </ul>
 */
@NullMarked
public enum AbsFn implements FluentFunction {


    ABS;

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        FluentFunction.ensureInput( parameters );

        final var biConsumer = FluentFunction.mapOrPassthrough( Number.class, AbsFn::abs );
        return parameters.positionals().mapMulti( biConsumer ).toList();
    }


    private static FluentValue<?> abs(Number number) {
        if (number instanceof Long v) {
            return FluentNumber.of( Math.abs( v ) );
        } else if (number instanceof Double v) {
            return FluentNumber.of( Math.abs( v ) );
        } else if (number instanceof BigDecimal v) {
            return FluentNumber.of( v.abs() );
        }
        throw new IllegalStateException( String.valueOf( number ) );
    }

}
