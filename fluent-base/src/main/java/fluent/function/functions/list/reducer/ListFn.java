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
package fluent.function.functions.list.reducer;

import com.ibm.icu.text.ListFormatter;
import fluent.bundle.resolver.Scope;
import fluent.function.*;
import fluent.types.FluentError;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/// LIST(): the reducer and formatter of lists. An implicit function.
///
/// LIST() reduces (by concatenation) all positional arguments to a single String, after applying the appropriate
/// formatting to each item. Heterogeneous lists are supported.
///
/// Note that selection will not work on lists.
///
/// LIST() options are as follows:
///
///   - `type:` valid types are `"and", "or", "unit"`. `"and"` is the default type.
///   - `width:` `"narrow", "short", or "wide"`. `"narrow"` is the default.
///     Examples: (English locale): narrow:"A, B, C"; short:"A, B & C"; wide:"A, B, and C"
///
@NullMarked
public enum ListFn implements FluentFunctionFactory<FluentFunction.TerminalReducer> {

    LIST;


    @Override
    public FluentFunction.TerminalReducer create(final Locale locale, final Options options) {
        final ListFormatter.Type type = options
                .asEnum( ListFormatter.Type.class, "type" )
                .orElse( ListFormatter.Type.AND );

        // example for English locale: NARROW:'1, 2, 3' vs. SHORT:'1, 2, & 3' or WIDE:'1, 2, and 3'
        final ListFormatter.Width width = options
                .asEnum( ListFormatter.Width.class, "width" )
                .orElse( ListFormatter.Width.NARROW );

        return new LFN( ListFormatter.getInstance( locale, type, width ) );
    }

    @Override
    public boolean canCache() {
        return true;
    }


    private record LFN(ListFormatter listFormatter) implements FluentFunction.TerminalReducer {


        @Override
            public String reduce(List<FluentValue<?>> list, Scope scope) throws FluentFunctionException {
            if (list.size() == 1) {
                    final FluentValue<?> fluentValue = list.getFirst();
                    return scope.registry().implicitFormat( fluentValue, scope );
                } else if (list.isEmpty()) {
                    return "";
                }

                // no overriding options. just use options (if any) in scope
                return reduceToString( list.stream(), scope );
            }

            ///  NOTE: the returned List of FluentValues must ONLY contain FluentErrors or FluentStrings.
            @Override
            public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
                if (parameters instanceof ResolvedParameters.SingleItem(List<FluentValue<?>> list)) {
                    return switch (parameters.singleValue()) {
                        // pass through w/o copy
                        case FluentString _ -> list;
                        case FluentError _ -> list;
                        // format
                        default ->
                                List.of( FluentString.of( scope.registry().implicitFormat( parameters.singleValue(), scope ) ) );
                    };
                } else if (parameters == ResolvedParameters.EMPTY) {
                    return List.of( FluentString.of( "" ) );
                } else {
                    return List.of( FluentString.of( reduceToString( parameters.positionals(), scope ) ) );
                }
            }

            private String reduceToString(final Stream<FluentValue<?>> stream, final Scope scope) {
                final List<String> formattedItems = stream
                        .map( v -> scope.registry().implicitFormat( v, scope ) )
                        .toList();

                return listFormatter.format( formattedItems );
            }
        }


}
