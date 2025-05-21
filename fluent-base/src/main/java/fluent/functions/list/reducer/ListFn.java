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

package fluent.functions.list.reducer;

import com.ibm.icu.text.ListFormatter;
import fluent.bundle.resolver.Scope;
import fluent.functions.FluentFunctionException;
import fluent.functions.Options;
import fluent.functions.ResolvedParameters;
import fluent.functions.TerminalReducer;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.stream.Stream;


/**
 * LIST() : the reducer and formatter of lists. An implicit function.
 * <p>
 * LIST() reduces (by concatenation) all positionals arguments to a single string, after applying the appropriate
 * formatting to each item. Heterogeneous lists are supported.
 * <p>
 * LIST() options are as follows:
 * <ul>
 *     <li>{@code type:} valid types are {@code "and", "or", "unit"}. {@code "and"} is the default type.
 *     <li>{@code width:} {@code "narrow", "short", or "wide"}. {@code "narrow"} is the default.
 *     Examples: (English locale): narrow:"A, B, C"; short:"A, B & C"; wide:"A, B, and C"
 * </ul>
 */
@NullMarked
public enum ListFn implements TerminalReducer {


    LIST;


    private static String reduceToString(final Stream<FluentValue<?>> stream, final Scope scope, final Options optIn) {
        final Options options = scope.options().mergeOverriding( optIn );
        // use options.asEnum(), but if not provided, use default
        // 'type'; 'width'  [unit type not supported? or should it be]
        // consider caching the default listformatter (?)
        // ICU does maintain a cache
        final ListFormatter.Type type = options
                .asEnum( ListFormatter.Type.class, "type" )
                .orElse( ListFormatter.Type.AND );

        // example for English locale: NARROW:'1, 2, 3' vs. SHORT:'1, 2, & 3' or WIDE:'1, 2, and 3'
        final ListFormatter.Width width = options
                .asEnum( ListFormatter.Width.class, "width" )
                .orElse( ListFormatter.Width.NARROW );

        final ListFormatter listFormatter = ListFormatter.getInstance( scope.bundle().locale(), type, width );
        final List<String> formattedItems = stream
                .map( v -> scope.formatter().format( v, scope ) ).toList();
        return listFormatter.format( formattedItems );
    }


    // this would occur for implicits
    @Override
    public String reduce(final List<FluentValue<?>> list, final Scope scope) throws FluentFunctionException {
        // fast path (no list; single value)
        if (list.size() == 1) {
            final FluentValue<?> fluentValue = list.getFirst();
            return scope.formatter().format( fluentValue, scope );
        } else if (list.isEmpty()) {
            return "";
        }

        // no overriding options. just use options (if any) in scope
        return reduceToString( list.stream(), scope, scope.options() );
    }

    // this would occur if we do something like LIST($anum), LIST($anum, $bval),
    // or LIST($anum, COUNT($avalue), type:or), etc.
    @Override
    public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) throws FluentFunctionException {
        // fast path (no list; single value)
        if (parameters.isSingle()) {
            return List.of( FluentString.of( scope.formatter().format( parameters.singleValue(), scope ) ) );
        } else if (parameters.positionalCount() == 0) {
            return List.of( FluentString.of( "" ) );
        }

        return List.of( FluentString.of( reduceToString( parameters.positionals(), scope, parameters.options() ) ) );
    }




    /*
    TODO: handle select!!

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
                .map( v -> FluentValueFormatter.select( v,selectExpression, params, initialScope ) )
                .map( v -> Resolver.resolve(v.value(),  initialScope ) )
                .flatMap( List::stream )
                .toList();
    }
*/


}
