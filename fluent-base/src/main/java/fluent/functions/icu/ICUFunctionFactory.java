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

package fluent.functions.icu;

import fluent.functions.FluentFunction;
import fluent.functions.FluentFunctionFactory;
import fluent.functions.FluentImplicit;
import fluent.functions.FunctionResources;
import fluent.functions.icu.list.CountFn;
import fluent.functions.icu.list.JoinFn;
import fluent.functions.icu.list.NumSortFn;
import fluent.functions.icu.list.StringSortFn;
import fluent.functions.icu.numeric.*;
import fluent.functions.icu.string.CaseFn;
import fluent.functions.icu.temporal.TemporalFn;

import java.util.Locale;
import java.util.Set;

/**
 *  The factory for ICU-based functions.
 *  <p>
 *      This factory is used to initialize fluent bundles with ICU-based functions.
 *  </p>
 *  <p>
 *      Example usage:
 *      {@code FluentBundle.builder( Locale.US, ICUFunctionFactory.INSTANCE ) }
 *  </p>
 *
 *
 */
public enum ICUFunctionFactory implements FluentFunctionFactory {

    INSTANCE;

    final private Set<FluentImplicit> implicits;
    final private Set<FluentFunction> functions;


    ICUFunctionFactory() {
        implicits = Set.of(
                new NumberFn(),
                new JoinFn(),
                new TemporalFn()
        );

        functions = Set.of(
                // list
                new CountFn(),
                new NumSortFn(),
                new StringSortFn(),
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
        return new ICUPluralSelector(locale);
    }


}
