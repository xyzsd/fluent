/*
 *
 *  Copyright (C) 2021-2025, xyzsd (Zach Del)
 *  Licensed under either of:
 *
 *    Apache License, Version 2.0
 *       (see LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0)
 *    MIT license
 *       (see LICENSE-MIT or http://opensource.org/licenses/MIT)
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
package fluent.function.functions.list;

import fluent.bundle.resolver.Scope;
import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import fluent.function.ResolvedParameters;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Locale;

/// COUNT(): Counts the supplied arguments.
///
/// If supplied arguments are Lists, items in the lists are also counted.
/// Count is not Locale sensitive and has no options.
///
/// Examples:
///    - `COUNT()` => 0
///    - `COUNT("hello")` => 1
///    - `COUNT(7)` => 1
///    - `COUNT("hello", 7)` => 2
///    - `COUNT($myvar)` where `$myvar = List.of("item","another","yet another")` => 3
///    - `COUNT($myvar, "hello")` where `$myvar = List.of("item","another","yet another")` => 4
///
///  This is a reducing function. Count returns a single number (FluentLong) as its output.
///  If any arguments are errors, an error message will be output.
@NullMarked
public enum CountFn implements FluentFunctionFactory<FluentFunction.Transform>, FluentFunction.Transform {


    COUNT;


    @Override
    public FluentFunction.Transform create(Locale locale, Options options) {
        // neither Locale nor Options are applicable
        return COUNT;
    }

    @Override
    public boolean canCache() {
        return false;   // no need to cache
    }

    @Override
    public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
        if (parameters.isEmpty()) {
            return List.of( FluentNumber.of( 0L ) );
        }

        // invalid values (FluentError) result in an exception
        parameters.positionals().forEach( FluentFunction::validate );

        return List.of( FluentNumber.of(
                parameters.positionals().count()
        ) );
    }
}

