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
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.text.NumberFormat;
import java.util.List;

/**
 * CURRENCY() : Format a number as a currency.
 * <p>
 * This does not support patterns. If a specific pattern is desired, use the {@link DecimalFn} function instead.
 * This will behave as NUMBER($var, style:"currency") but is more explicit.
 * </p>
 * <p>
 * Non-numeric values are passed through. At least one value is required.
 * </p>
 * <p>
 * Options:
 *     <ul>
 *         <li>{@code minimumFractionDigits:} integer value (0 is default) </li>
 *     </ul>
 */
@NullMarked
public enum CurrencyFn implements FluentFunction {

    CURRENCY;

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        FluentFunction.ensureInput( parameters );

        final NumberFormat fmt = NumberFormat.getCurrencyInstance( scope.bundle().locale() );

        fmt.setMinimumFractionDigits( parameters.options().asInt( "minimumFractionDigits" )
                .orElse( fmt.getMinimumFractionDigits() )
        );

        final var biConsumer = FluentFunction.mapOrPassthrough( Number.class, fmt::format );
        return parameters.positionals().mapMulti( biConsumer ).toList();
    }
}