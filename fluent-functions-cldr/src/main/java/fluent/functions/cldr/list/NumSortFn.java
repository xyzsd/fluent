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

package fluent.functions.cldr.list;

import fluent.functions.FluentFunction;
import fluent.functions.FluentFunctionException;
import fluent.functions.ResolvedParameters;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 *  NUMSORT()
 *  <p>
 *  Sort a list of numbers.
 *  <p>
 *  NUMSORT() sorts numbers, first by converting them to BigDecimals and then sorting.
 *  Specific formatting rules--if desired--can be applied after NUMSORT().
 *  <p>
 *      For example, {@code NUMBER( NUMSORT($numList), minimumSignificantDigits:6) }
 *  <p>
 *  Options:
 *  <ul>
 *      <li>{@code order}: may be {@code "ascending"} (the default) or {@code "descending"}</li>
 *  </ul>
 *  <p>
 *      NUMSORT() will error on non-numeric input. e.g., {@code NUMSORT(3, 2, 1, "barf") } will result in an error.
 */
public class NumSortFn implements FluentFunction {

    public static final String NAME = "NUMSORT";

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters params, final Scope scope) {
        FluentFunction.ensureInput( params );

        final Order order = params.options().asEnum( Order.class, "order" )
                .orElse( Order.ASCENDING );

        return params.valuesAll()
                .map( NumSortFn::toBigDecimal )
                .sorted( order.comparator() )
                .<FluentValue<?>>map( FluentNumber::of )
                .toList();
    }


    private static BigDecimal toBigDecimal(FluentValue<?> in) {
        if(in instanceof FluentNumber<?> fluentNumber) {
            return fluentNumber.asBigDecimal();
        }

        throw FluentFunctionException.create(
                "Expected numeric value, not non-numeric FluentValue: '%s'", in
        );
    }


    private enum Order {

        ASCENDING(Comparator.naturalOrder()),
        DESCENDING(Comparator.reverseOrder());

        private final Comparator<BigDecimal> cmp;

        Order(Comparator<BigDecimal> comparator) {
            this.cmp = comparator;
        }

        Comparator<BigDecimal> comparator() {
            return cmp;
        }
    }
}
