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
import fluent.functions.FluentFunctionException;
import fluent.functions.ResolvedParameters;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;

import java.util.List;

/**
 * IADD(): Integral-add function
 * <p>
 * Adds the named option "addend" (which must be a long or integer) to each positional value.
 *
 * </p>
 * <p>
 * Examples:
 *     <ul>
 *         <li>IADD(), IADD("stringvalue", 1), IADD(3.3, 4.4, addend:1), IADD(5, 2, addend:3.3) -> error</li>
 *         <li>IADD(5, addend:1) -> 6</li>
 *         <li>IADD(5, addend:-1) -> 4</li>
 *         <li>IADD($var, addend:1) -> adds 1 to each item in {@code var}</li>
 *     </ul>
 * <p>
 * No 'passthrough' occurs with this method. For any arguments that are NOT integers or longs, an error will occur.
 */
public class AddFn implements FluentFunction {

    // math terminology refresher: 'augend' + 'addend' = 'sum'

    public static final String NAME = "IADD";   // so very Excel

    public AddFn() {}

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters params, final Scope scope) {
        FluentFunction.ensureInput( params );
        final long addend = params.options()
                .asLong( "addend" )
                .orElseThrow(
                        () -> FluentFunctionException.create( "Missing required option 'addend'" )
                );

        return params.valuesAll()
                .peek( FluentFunction::validate )
                .<FluentValue<?>>map( v -> asLongAndAdd( v, addend ) )
                .toList();
    }


    private static FluentValue<?> asLongAndAdd(final FluentValue<?> in, final long addend) {
        final Long augend = FluentFunction.asFluentValue( FluentNumber.FluentLong.class, in ).value();
        return FluentNumber.of( augend + addend );
    }

}

