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

import java.util.List;

/**
 * IADD(): Integral-add function
 * <p>
 * Adds the named option "addend" (which must be a long or integer) to each positionals value.
 * <p>
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
@NullMarked
public enum AddFn implements FluentFunction {

    // math terminology refresher: 'augend' + 'addend' = 'sum'
    IADD; // so very Excel



    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        FluentFunction.ensureInput( parameters );

        final long addend = parameters.options()
                .asLong( "addend" )
                .orElseThrow(
                        () -> FluentFunctionException.of( "Missing required option 'addend'" )
                );

        return parameters.positionals()
                .mapToLong( AddFn::toLongFn )
                .map( augend -> augend + addend)
                .<FluentValue<?>>mapToObj( FluentNumber::of)
                .toList();
    }

    private static long toLongFn(final FluentValue<?> in) {
        if(in instanceof FluentNumber.FluentLong(Long value)) {
            return value;
        }
        throw FluentFunctionException.of(String.format("Invalid type: '%s':'%s'", in.getClass(), in));
    }

}

