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

import java.util.List;
import java.util.Locale;

/// ## OFFSET()
/// Integral-offset function. Offsets each positional argument by a fixed integer amount.
///
/// ## Examples
/// {@snippet :
///       - OFFSET(5, increment:1) -> 6
///       - OFFSET(5, decrement:1) -> 4
///       - OFFSET(3,5,7, decrement:1) -> '2, 4, 6' (subtracts 1 from each item in `var`)
///       - OFFSET($var, decrement:1) -> subtracts 1 from each item in `var`
///       - OFFSET(5, decrement:-1) -> 6        // though this is not recommended! use 'increment' instead
///       - OFFSET()                                  // ERROR : 'increment' or 'decrement' not specified
///       - OFFSET("stringvalue", increment:1),       // ERROR : string pattern; must be an integral type
///       - OFFSET(3.3, increment:1)                  // ERROR : double pattern; must be an integral type
///       - OFFSET(3, increment:1.113)                // ERROR : increment or decrement MUST be integral type
///       - OFFSET(3, increment:1, decrement:3)       // ERROR : can only have either 'increment' or 'decrement'
/// }
///
/// No 'passthrough' occurs with this method. If any positional argument is NOT an integral type (integer or long),
/// an error will occur.
@NullMarked
public enum OffsetFn implements FluentFunctionFactory<FluentFunction.Transform> {

    // not locale sensitive
    OFFSET;

    private static final String INCREMENT = "increment";
    private static final String DECREMENT = "decrement";

    @Override
    public FluentFunction.Transform create(final Locale locale, final Options options) {
        if (options.size() != 1 || (!options.has( INCREMENT ) && !options.has( DECREMENT ))) {
            throw FluentFunctionException.of( "Expected exactly one option, either '%s' or '%s'", INCREMENT, DECREMENT );
        }

        final long offset = options.asLong( INCREMENT )
                .orElseGet( () -> -options.asLong( DECREMENT ).orElseThrow() );

        return new Offset( offset );
    }

    @Override
    public boolean canCache() {
        return true;
    }


    private static class Offset implements FluentFunction.Transform {
        private final long offset;

        private Offset(long offset) {
            this.offset = offset;
        }

        private static long toLong(final FluentValue<?> in) {
            if (in instanceof FluentNumber.FluentLong(Long value)) {
                return value;
            }
            throw FluentFunctionException.of( String.format( "Invalid type: received a %s<%s>; expected a FluentLong",
                    in.getClass().getSimpleName(),
                    in.value().getClass().getSimpleName() )
            );
        }


        @Override
        public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
            FluentFunction.ensureInput( parameters );

            return parameters.positionals()
                    .mapToLong( Offset::toLong )
                    .map( value -> value + offset )
                    .<FluentValue<?>>mapToObj( FluentNumber::of )
                    .toList();
        }

    }

}
