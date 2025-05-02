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

package fluent.functions.cldr;

import fluent.functions.FluentFunction;
import fluent.functions.FluentFunctionFactory;
import fluent.functions.FluentImplicit;
import fluent.functions.FunctionResources;
import fluent.functions.cldr.list.CountFn;
import fluent.functions.cldr.list.JoinFn;
import fluent.functions.cldr.list.NumSortFn;
import fluent.functions.cldr.list.StringSortFn;
import fluent.functions.cldr.numeric.*;
import fluent.functions.cldr.string.CaseFn;
import fluent.functions.cldr.temporal.TemporalFn;

import java.util.Locale;
import java.util.Set;

/**
 *  The factory for CLDR-based functions.
 *  <p>
 *      This factory is used to initialize fluent bundles with CLDR-based functions.
 *  </p>
 *  <p>
 *      Example usage:
 *      {@code FluentBundle.builder( Locale.US, CLDRFunctionFactory.INSTANCE ) }
 *  </p>
 *
 *
 */
@Deprecated(since = "0.71", forRemoval = true)
public enum CLDRFunctionFactory implements FluentFunctionFactory {

    INSTANCE;

    final private Set<FluentImplicit> implicits;
    final private Set<FluentFunction> functions;


    CLDRFunctionFactory() {
        implicits = Set.of(
            new NumberFn(),
            new JoinFn(),
            new TemporalFn()
        );

        functions = Set.of(
                // list
                new CountFn(),
                new StringSortFn(),
                new NumSortFn(),
                // numeric
                new AbsFn(),
                new AddFn(),
                new CompactFn(),
                new CurrencyFn(),
                new DecimalFn(),
                new SignFn(),
                // string
                new CaseFn()
        );
    }

    @Override
    public Set<FluentImplicit> implicits() {
        return implicits;
    }

    @Override
    public Set<FluentFunction> functions() {
        return functions;
    }


    @Override
    public FunctionResources resources(Locale locale) {
        return new CLDRPluralSelector(locale);
    }

}
