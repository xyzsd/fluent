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
package fluent.function.functions.list;


import fluent.bundle.resolver.Scope;
import fluent.function.*;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/// ## NUMSORT()
/// Locale-aware Number sorting
///
/// `NUMSORT()` sorts numbers, first by converting them to BigDecimals and then sorting.
/// Specific formatting rules, if desired, can be applied after NUMSORT().
///
/// For example, `NUMBER( NUMSORT($numList), minimumSignificantDigits:6)`
///
/// Note that the inverse `NUMSORT(NUMBER(...))` will not work, because NUMBER is a formatter that converts
/// numeric values to their text (String) representation.
///
/// Options:
/// - `order`: either `"ascending"` (the default) or `"descending"`
///
/// ## Examples
/// {@snippet :
///     NUMSORT(3,2,1, order:"ascending")  // '1,2,3'
/// }
///
/// NUMSORT() will error on non-numeric input; for example, `NUMSORT(3, 2, 1, "barf")` will result in an error.
@NullMarked
public enum NumSortFn implements FluentFunctionFactory<FluentFunction.Transform> {

    // TODO: consider a passthrough version, perhaps selectable by an option flag. Numbers would be extracted from the
    //       input list and sorted. Then, output this list along with non-numeric values from the original input.
    // TODO: PERFORMANCE: consider single-value numeric input case; no need to sort.

    NUMSORT;


    @Override
    public FluentFunction.Transform create(Locale locale, Options options) {
        final SortOrder order = options.asEnum( SortOrder.class, "order" )
                .orElse( SortOrder.ASCENDING );

        return switch(order) {
            case ASCENDING -> NumSorter.NS_ASCENDING;
            case DESCENDING -> NumSorter.NS_DESCENDING;
        };
    }

    @Override
    public boolean canCache() {
        // no need to cache
        return false;
    }


    private record NumSorter(Comparator<BigDecimal> comparator) implements FluentFunction.Transform {
        static final NumSorter NS_ASCENDING = new NumSorter(Comparator.naturalOrder());
        static final NumSorter NS_DESCENDING = new NumSorter(Comparator.reverseOrder());

        @Override
        public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
            return parameters.positionals()
                    .map( NumSorter::toBigDecimal )
                    .sorted( comparator )
                    .<FluentValue<?>>map( FluentNumber.FluentBigDecimal::new )
                    .toList();
        }

        private static BigDecimal toBigDecimal(FluentValue<?> in) {
            if(in instanceof FluentNumber<?> fluentNumber) {
                return fluentNumber.asBigDecimal();
            }

            throw FluentFunctionException.of(
                    "Expected FluentNumber<>, not non-numeric FluentValue: '%s'", in
            );
        }
    }


    private enum SortOrder {
        ASCENDING, DESCENDING
    }
}
