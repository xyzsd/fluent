/*
 *
 *  Copyright (C) 2021-2025, xyzsd (Zach Del) 
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
import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import fluent.function.ResolvedParameters;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/// STRINGSORT() items
///
/// STRINGSORT is a non-reducing string formatting and string sorting function.
/// STRINGSORT works in the following manner:
///   <ol>
///     -
///   For each non-String item, convert to a String using the implicit formatter for the type.
///   NOTE: if specific formatting is preferred, it should be performed prior to sorting. For example,
///   `STRINGSORT(NUMBER($pattern, $anotherValue))`.
///
///     -
///   Sort the String values and return the list of values.
///
///   </ol>
///
///   To reiterate, all values emitted from a Sort will be String values. Therefore, if (e.g., numeric) type
///   manipulation is needed, it must be performed prior to sorting.
///
///   Note: STRINGSORT() is a String-based sort. Therefore, it is not generally appropriate to sort numbers, since
///   it will do so following number formatting.
///
///   STRINGSORT() options:
///
///     -
///   `order:` "natural" or "reversed". Default is natural. Reverses sort order if "reversed".
///
///     -
///   `strength:` sorting strength: "primary","secondary","tertiary","identical". The meanings
///   of these are Locale-dependent.
///
/// - `primary:` different base letters considered different (e.g., "a" vs. "b")
///             - `secondary:` different accented forms considered different
///             - `tertiary:` e.g.: case differences "a" vs "A"
///             - `identical:` all character differences are considered unique
///
///
///     -
///   `decomposition:` sorting decomposition: "none","full","canonical". For languages with
///   accented characters, canonical or full decomposition should be used.
///
/// - `none:` accented characters are not decomposed
///             - `canonical:` decompose characters with canonical variants (form D)
///             - `full:` decompose canonical variants and Unicode compatibility variants (form KD)
///
///
///
@NullMarked
public enum StringSortFn implements FluentFunctionFactory<FluentFunction.Transform> {

    STRINGSORT;

    // Sorting converts inputs to FluentString prior to sort. The reason is that
    // formatting may affect sort order. However... sorting strings may not always
    // sort numbers correctly.

    // if numbers AND strings need to be sorted, NUMSORT could be called prior to STRINGSORT
    // and some order would be preserved.


    @Override
    public FluentFunction.Transform create(final Locale locale, final Options options) {
        final Order order = options.asEnum( Order.class, "order" )
                .orElse( Order.NATURAL );
        final Strength strength = options.asEnum( Strength.class, "strength" )
                .orElse( Strength.PRIMARY );
        final Decomposition decomposition = options.asEnum( Decomposition.class, "decomposition" )
                .orElse( Decomposition.NONE );

        final Collator col = Collator.getInstance( locale );
        col.setStrength( strength.ordinal() );
        col.setDecomposition( decomposition.ordinal() );

        // custom comparator
        Comparator<FluentString> comparator = (fs1, fs2) -> col.compare( fs1.value(), fs2.value() );
        comparator = (order == Order.REVERSED) ? comparator.reversed() : comparator;

        return new StringSorter( comparator );
    }

    @Override
    public boolean canCache() {
        return true;
    }

    private enum Strength {
        // low to high
        PRIMARY, SECONDARY, TERTIARY, IDENTICAL
    }

    private enum Decomposition {
        NONE, CANONICAL, FULL
    }


    private enum Order {
        NATURAL,
        REVERSED
    }

    private record StringSorter(Comparator<FluentString> comparator) implements FluentFunction.Transform {
        private static FluentString convert(final FluentValue<?> in, final Scope scope) {
            if (in instanceof FluentString fs) {
                return fs;
            } else {
                return FluentString.of( scope.registry().implicitFormat( in, scope ) );
            }
        }

        @Override
        public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
            return parameters.positionals()
                    .map( v -> convert( v, scope ) )
                    .sorted( comparator )
                    .<FluentValue<?>>map( Function.identity() )
                    .toList();
        }
    }


}
