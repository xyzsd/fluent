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
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * JOIN() : the reducer and formatter of lists. An implicit function.
 * <p>
 * JOIN() reduces (by concatenation) all positional arguments to a single string, after applying the appropriate
 * formatting to each item. Heterogeneous lists are supported.
 * </p>
 * <p>
 * JOIN() options are as follows:
 *     <ul>
 *         <li>{@code separator:} String value to insert between items. The default separator is {@code ", "}</li>
 *         <li>{@code junction:} Optional String value (and separator) to insert before the final item (e.g., ", and " or " and ")</li>
 *         <li>{@code pairSeparator:} Optional; only applies if there are 2 values (and has precedence over 'junction').
 *         String value to separate items. (e.g., " and ")</li>
 *     </ul>
 */
public class JoinFn implements FluentImplicit, ImplicitReducer {

    private static final String DEFAULT_SEPARATOR = ", ";


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
    public List<FluentValue<?>> select(final SelectExpression selectExpression,
                                       final ResolvedParameters params,
                                       final Scope initialScope) {
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
        // fast path(s)
        {
            if (params.noPositionals()) {
                return "";
            }

            final FluentValue<?> single = hasSinglePositional( params );
            if (single != null) {
                return single.format( scope );
            }
        }

        // simple case: use default separator and join [reduce]
        // also handles the single-argument case
        if (params.options().isEmpty()) {
            return params.valuesAll()
                    .map( v -> v.format( scope ) )
                    .filter( Predicate.not( String::isEmpty ) )     // possibly superfluous
                    .collect( Collectors.joining( DEFAULT_SEPARATOR ) );
        }

        // all other cases
        final String separator = params.options().asString( "separator" )
                .orElse( DEFAULT_SEPARATOR );
        final String junction = params.options().asString( "junction" )
                .orElse( DEFAULT_SEPARATOR );
        final String pairSeparator = params.options().asString( "pairSeparator" )
                .orElse( junction );

        final List<FluentValue<?>> list = params.valuesAll().toList();

        assert (list.size() > 1);

        StringBuilder sb = new StringBuilder( 128 );   // TODO: appropriate sizing

        if (list.size() == 2) {
            sb.append( list.get( 0 ).format( scope ) );
            sb.append( pairSeparator );
            sb.append( list.get( 1 ).format( scope ) );
        } else {
            final int sizeM2 = list.size() - 2;
            for (int i = 0; i < sizeM2; i++) {
                sb.append( list.get( i ).format( scope ) );
                sb.append( separator );
            }
            sb.append( list.get( sizeM2 ).format( scope ) );
            sb.append( junction );
            sb.append( list.get( sizeM2 + 1 ).format( scope ) );
        }

        return sb.toString();
    }

    private static @Nullable FluentValue<?> hasSinglePositional(ResolvedParameters params) {
        if ((params.positionalCount() == 1) && (params.valueCount( Positional.FIRST ) == 1)) {
            return params.valueFirst( Positional.FIRST ).orElseThrow();
        }
        return null;
    }
}
