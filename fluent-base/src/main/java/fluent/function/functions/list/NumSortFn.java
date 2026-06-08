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
import java.util.*;

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
/// Implementation note: finite numeric values will be converted to BigDecimals. Non-finite values will always
/// remain Non-finite values of Double. For natural ordering, NaN is considered greater than positive infinity.
///
///
/// ## Examples
/// {@snippet :
///     NUMSORT(3,2,1, order:"ascending")  // '1,2,3'
///}
///
/// NUMSORT() will error on non-numeric input; for example, `NUMSORT(3, 2, 1, "barf")` will result in an error.
@NullMarked
public enum NumSortFn implements FluentFunctionFactory<FluentFunction.Transform> {

    // TODO: consider a passthrough version, perhaps selectable by an option flag. Numbers would be extracted from the
    //       input list and sorted. Then, output this list along with non-numeric values from the original input.
    //       The question would be where to place the non-numeric values in the output list.
    //

    NUMSORT;


    @Override
    public FluentFunction.Transform create(Locale locale, Options options) {
        final SortOrder order = options.asEnum( SortOrder.class, "order" )
                .orElse( SortOrder.ASCENDING );

        return switch (order) {
            case ASCENDING -> NumSorter.NS_ASCENDING;
            case DESCENDING -> NumSorter.NS_DESCENDING;
        };
    }

    @Override
    public boolean canCache() {
        // no need to cache
        return false;
    }


    private record NumSorter(boolean reverse) implements FluentFunction.Transform {
        static final NumSorter NS_ASCENDING = new NumSorter( false );
        static final NumSorter NS_DESCENDING = new NumSorter( true );

        @Override
        public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
            // singlet? no need to sort.
            if (parameters.isSingle()) {
                return parameters.firstPositional();
            }

            // count nonfinite values and ignore for now; then then add back to list.
            int nNegInf = 0;
            int nPosInf = 0;
            int nNaN = 0;

            final List<FluentValue<?>> input = parameters.positionals().toList();
            final List<BigDecimal> bigDecimals = new ArrayList<>( input.size() );

            for (FluentValue<?> v : input) {
                switch (v.value()) {
                    case BigDecimal bd -> bigDecimals.add( bd );
                    case Long l -> bigDecimals.add( BigDecimal.valueOf( l ) );
                    case Double d when Double.isFinite( d ) -> bigDecimals.add( BigDecimal.valueOf( d ) );
                    case Double d when Double.isInfinite( d ) -> {
                        if (Double.isInfinite( d ) && d > 0) {
                            nPosInf++;
                        } else {
                            nNegInf++;
                        }
                    }
                    case Double d when Double.isNaN( d ) -> nNaN++;
                    default -> throw FluentFunctionException.of(
                            "Expected a FluentNumber<>, but encountered a non-numeric FluentValue: '%s'", v
                    );
                }
            }

            bigDecimals.sort( Comparator.naturalOrder() );

            final List<FluentValue<?>> output = new ArrayList<>(input.size());

            // arrange items in natural order, as per JDK Double.compare() (which for natural ordering places
            // NaN after positive infinity; we keep that approach here.

            for (int i = 0; i < nNegInf; i++) {
                output.add( FluentNumber.of( Double.NEGATIVE_INFINITY) );
            }

            for (BigDecimal bd : bigDecimals) {
                output.add( FluentNumber.of( bd ) );
            }

            for (int i = 0; i < nPosInf; i++) {
                output.add( FluentNumber.of( Double.POSITIVE_INFINITY) );
            }

            for (int i = 0; i < nNaN; i++) {
                output.add( FluentNumber.of( Double.NaN) );
            }

            // reverse the order if requested.
            if (reverse) {
                Collections.reverse( output );
            }

            return output;
        }
    }


    private enum SortOrder {
        ASCENDING, DESCENDING
    }
}
