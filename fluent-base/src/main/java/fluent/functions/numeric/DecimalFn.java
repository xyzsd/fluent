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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;


/**
 * DECIMAL() : Format a number, optionally using a specific format pattern.
 * <p>
 * Non-numeric values are passed through. At least one value is required.
 * </p>
 * <p>
 *     This behaves identically to NUMBER() if no pattern is specified.
 * </p>
 * <p>
 * Options:
 *     <ul>
 *          <li>{@code pattern:} Decimal format pattern (as per DecimalFormat class)</li>
 *          <li>{@code minimumFractionDigits:} integer value (0 is default) </li>
 *     </ul>
 * <p>
 *     Example: {@code DECIMAL($num, pattern:"###.00"}
 * </p>
 */
@NullMarked
public enum DecimalFn implements FluentFunction {

    DECIMAL;


    // extraneous options ignored but extraneous args are errors
    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        FluentFunction.ensureInput( parameters );

        // get a localized DecimalFormat [it is an internal error if DecimalFormat not the concrete class]
        final DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance( scope.bundle().locale() );

        df.setMinimumFractionDigits( parameters.options().asInt( "minimumFractionDigits" )
                .orElse( df.getMinimumFractionDigits() )
        );

        parameters.options().asString( "pattern" )
                .ifPresent( pattern -> applyPattern( df, pattern ) );

        final var biConsumer = FluentFunction.mapOrPassthrough( Number.class, df::format );
        return parameters.positionals().mapMulti( biConsumer ).toList();
    }


    private static void applyPattern(final DecimalFormat df, final String pattern) {
        try {
            // NOTE: an argument could be made for 'applyLocalizedPattern()' here, but I believe toPattern()
            //       is a better fit for the general case. This could be changed via an option.
            df.applyPattern( pattern );
        } catch (IllegalArgumentException e) {
            throw FluentFunctionException.of( "Invalid format pattern '%s'",
                    pattern );
        }
    }
}
