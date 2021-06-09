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
import fluent.types.FluentValue;

import java.text.CompactNumberFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 * COMPACT() : Compact numeric formatter
 * <p>
 * Format a number using the compact formatter.
 * </p>
 * <p>
 * Non-numeric values are passed through. At least one value is required.
 * </p>
 * <p>
 * Options:
 *     <ul>
 *         <li>{@code style:} {@code "short"} or {@code "long"} (default: {@code "short"})</li>
 *         <li>{@code minimumFractionDigits:} integer value (0 is default) </li>
 *     </ul>
 */
public class CompactFn implements FluentFunction {

    /**
     * Method name
     */
    public static final String NAME = "COMPACT";


    public CompactFn() {}

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters params, final Scope scope) {
        FluentFunction.ensureInput( params );

        final NumberFormat.Style style = params.options().asEnum( NumberFormat.Style.class, "style" )
                .orElse( NumberFormat.Style.SHORT );

        final int minFractionDigits = params.options().asInt( "minimumFractionDigits" ).orElse( 0 );

        final CompactNumberFormat fmt = (CompactNumberFormat) NumberFormat.getCompactNumberInstance(
                scope.bundle().locale(), style );

        // negative values replaced with 0 per CompactNumberFormat spec
        fmt.setMinimumFractionDigits( minFractionDigits );

        return FluentFunction.mapOverNumbers( params.valuesAll(),
                scope, fmt::format );
    }


}
