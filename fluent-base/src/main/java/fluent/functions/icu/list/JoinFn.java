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

import com.ibm.icu.text.ListFormatter;
import fluent.functions.FluentImplicit;
import fluent.functions.ImplicitReducer;
import fluent.functions.ResolvedParameters;
import fluent.functions.ResolvedParameters.Positional;
import fluent.syntax.AST.SelectExpression;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * JOIN() : the reducer and formatter of lists. An implicit function.
 * <p>
 * JOIN() reduces (by concatenation) all positional arguments to a single string, after applying the appropriate
 * formatting to each item. Heterogeneous lists are supported.
 * <p>
 * JOIN() options are as follows:
 *     <ul>
 *         <li>{@code type:} valid types are {@code "and", "or", "unit"}. {@code "and"} is the default type.
 *         <li>{@code width:} {@code "narrow", "short", or "wide"}. {@code "narrow"} is the default.
 *         Examples: (English locale): narrow:"A, B, C"; short:"A, B & C"; wide:"A, B, and C"
 *     </ul>
 */
public class JoinFn implements FluentImplicit, ImplicitReducer {

    @Override
    public Implicit id() {
        return Implicit.JOIN;
    }


    @Override
    public List<FluentValue<?>> apply(ResolvedParameters params, Scope scope) {
        return List.of( FluentString.of( reduceToString( params, scope ) ) );
    }

    @Override
    public String reduce(final List<FluentValue<?>> in, final Scope scope) {
        if (in.size() == 1) {
            return in.get( 0 ).format( scope );
        } else if (in.isEmpty()) {
            return "";
        }

        return reduceToString( ResolvedParameters.from( in, scope ), scope );
    }


    @Override
    public List<FluentValue<?>> select(final SelectExpression selectExpression, final ResolvedParameters params, final Scope initialScope) {
        // For multiple values, we do NOT want to reduce prior to selection;
        // instead, we want to select() on each value in the list.
        //
        // What we do not do, but could do in the future would be to specify a way to get the current FluentValue<>
        // that the selector is on, when operating on multiple values. That special value would be mapped into a
        // new Scope with the Scope.rescope() method. For example:
        //      $myList = List<String> "first","second","third"...
        //
        //      example = $myList -> {
        //                [first] do this
        //                [second] do that
        //               *[other] $myList
        //      }
        // [other] will result in the entire list being printed. if we just want the current item, (e.g., "third") we would
        // need another identifier like '$$' or '$myList.current' or perhaps a function like CURRENT($myList) ....
        //

        return params.valuesAll()
                .map( v -> v.select( selectExpression, params, initialScope ) )
                .map( v -> v.value().resolve( initialScope ) )
                .flatMap( List::stream )
                .toList();
    }


    private static String reduceToString(final ResolvedParameters params, final Scope scope) {
        // fast paths
        {
            if (params.noPositionals()) {
                return "";
            }

            final FluentValue<?> single = hasSinglePositional( params );
            if (single != null) {
                return single.format( scope );
            }
        }

        // use options.asEnum(), but if not provided, use default
        // 'type'; 'width'  [unit type not supported? or should it be]
        // consider caching the default listformatter (?)
        // ICU does maintain a cache
        final ListFormatter.Type type = params.options()
                .asEnum(ListFormatter.Type.class, "type")
                .orElse(ListFormatter.Type.AND  );

        // for English locale: NARROW:'1, 2, 3' vs. SHORT:'1, 2, & 3' or WIDE:'1, 2, and 3'
        final ListFormatter.Width width = params.options()
                .asEnum(ListFormatter.Width.class, "width")
                .orElse(ListFormatter.Width.NARROW  );

        final ListFormatter listFormatter = ListFormatter.getInstance(scope.bundle().locale(), type, width);
        final List<String> formattedItems = params.valuesAll().map( v -> v.format( scope ) ).toList();
        return listFormatter.format( formattedItems );
    }

    private static @Nullable FluentValue<?> hasSinglePositional(ResolvedParameters params) {
        if ((params.positionalCount() == 1) && (params.valueCount( Positional.FIRST ) == 1)) {
            return params.valueFirst( Positional.FIRST ).orElseThrow();
        }
        return null;
    }
}
