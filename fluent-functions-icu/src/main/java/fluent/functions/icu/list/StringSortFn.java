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

package fluent.functions.icu.list;

import fluent.functions.FluentFunction;
import fluent.functions.ResolvedParameters;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentString;
import fluent.types.FluentValue;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * SORT() items
 * <p>
 * SORT is a non-reducing string formatting and string sorting function.
 * SORT works in the following manner:
 *     <ol>
 *         <li>
 *             For each non-String item, convert to a String using the implicit formatter for the type.
 *             NOTE: if specific formatting is preferred, it should be performed prior to sorting. For example,
 *             {@code SORT(NUMBER($value, $anotherValue))}.
 *         </li>
 *         <li>
 *             Sort the String values and return the list of values.
 *         </li>
 *     </ol>
 * <p>
 *     To reiterate, all values emitted from a Sort will be String values. Therefore, if (e.g., numeric) type
 *     manipulation is needed, it must be performed prior to sorting.
 * <p>
 *     Note: SORT() is a String-based sort. Therefore, it is not generally appropriate to sort numbers, since
 *     it will do so following number formatting.
 * <p>
 *     SORT() options:
 *     <ul>
 *         <li>
 *             {@code order:} "natural" or "reversed". Default is natural. Reverses sort order if "reversed".
 *         </li>
 *         <li>
 *             {@code strength:} sorting strength: "primary","secondary","tertiary","identical". The meanings
 *             of these are Locale-dependent.
 *             <ul>
 *                 <li>{@code primary:} different base letters considered different (e.g., "a" vs. "b")</li>
 *                 <li>{@code secondary:} different accented forms considered different</li>
 *                 <li>{@code tertiary:} e.g.: case differences "a" vs "A" </li>
 *                 <li>{@code identical:} all character differences are considered unique</li>
 *             </ul>
 *         </li>
 *         <li>
 *             {@code decomposition:} sorting decomposition: "none","full","canonical". For languages with
 *             accented characters, canonical or full decomposition should be used.
 *             <ul>
 *                 <li>{@code none:} accented characters are not decomposed</li>
 *                 <li>{@code canonical:} decompose characters with canonical variants (form D)</li>
 *                 <li>{@code full:} decompose canonical variants and Unicode compatibility variants (form KD)</li>
 *             </ul>
 *         </li>
 *
 *     </ul>
 */
public class StringSortFn implements FluentFunction {

    // Sorting converts inputs to FluentString prior to sort. The reason is that
    // formatting may affect sort order. However.. sorting strings may not always
    // sort numbers correctly.

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


    public static final String NAME = "STRINGSORT";

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters params, final Scope scope) {
        FluentFunction.ensureInput( params );

        final Order order  = params.options().asEnum( Order.class, "order" )
                .orElse( Order.NATURAL );
        final Strength strength = params.options().asEnum( Strength.class, "strength" )
                .orElse( Strength.PRIMARY );
        final Decomposition decomposition = params.options().asEnum( Decomposition.class, "decomposition" )
                .orElse( Decomposition.NONE );

        final Collator col = Collator.getInstance( scope.bundle().locale() );
        col.setStrength( strength.ordinal() );
        col.setDecomposition( decomposition.ordinal() );

        // custom comparator
        Comparator<FluentString> comparator = (fs1, fs2) -> col.compare( fs1.value(), fs2.value() );
        comparator = (order == Order.REVERSED) ? comparator.reversed() : comparator;

        return params.valuesAll()
                .map( v -> toFS( v, scope ) )
                .sorted( comparator )
                .<FluentValue<?>>map( Function.identity() )
                .toList();
    }


    private FluentString toFS(FluentValue<?> in, Scope scope) {
        if (in instanceof FluentString fs) {
            return fs;
        } else {
            return FluentString.of( in.format( scope ) );
        }
    }


}
