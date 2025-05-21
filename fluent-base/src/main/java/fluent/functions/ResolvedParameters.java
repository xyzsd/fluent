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

package fluent.functions;

import fluent.bundle.resolver.Resolver;
import fluent.bundle.resolver.Scope;
import fluent.syntax.AST.CallArguments;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/// Resolved Parameters for Fluent Functions.
//TODO: probably should limit # of positionals, to say 8 or so
// should be sufficient. each positionl can be a list
@NullMarked
public sealed interface ResolvedParameters
        permits RPImpl.RP0, RPImpl.RP11, RPImpl.RP1n, RPImpl.RPNn {

    ///  Constant ResolvedParameters without positionals arguments and without options
    ResolvedParameters RP_EMPTY = new RPImpl.RP0( Options.EMPTY );



    ///  Create a ResolvedParameters from a CallArguments AST node.
    static ResolvedParameters from(@Nullable CallArguments callArgs, final Scope scope) {
        requireNonNull( scope );

        // handle options (named arguments). Options.from() can handle null callArgs
        final Options mergedOptions = scope.options().mergeOverriding( Options.from( callArgs ) );

        // no callArgs
        if (callArgs == null) {
            return new RPImpl.RP0( mergedOptions );
        }

        return switch (callArgs.positionals().size()) {
            case 0 -> new RPImpl.RP0( mergedOptions );
            case 1 -> {
                final List<FluentValue<?>> list = Resolver.resolve( callArgs.positionals().getFirst(), scope );
                yield new RPImpl.RP1n( list, mergedOptions );
            }
            default -> {
                final List<List<FluentValue<?>>> listyList = callArgs.positionals().stream()
                        .map( expression -> Resolver.resolve( expression, scope ) ).toList();
                yield new RPImpl.RPNn( listyList, mergedOptions );
            }
        };
    }

    Options options();

    List<FluentValue<?>> positional(int index);

    List<FluentValue<?>> firstPositional();

    ///  `true` if there is at least one positional argument with one value.
    boolean hasPositionals();

    // stream ALL arguments, in order
    Stream<FluentValue<?>> positionals();

    int positionalCount();

    // true if single argument with single value (not a list); just one FluentValue<?>
    default boolean isSingle() {
        return false;
    }

    // if isSingle() == true, single, this returns a value, otherwise; no such element exception
    default FluentValue<?> singleValue() throws NoSuchElementException {
        throw new NoSuchElementException();
    }


    /*
    // typically used by FluentValue<> formatters
    // uses options supplied in scope -- if any
    public static ResolvedParametersNEW from(FluentValue<?> value, final Scope scope) {
        return new ResolvedParametersNEW( List.of(List.of(value)), scope.options() );
    }

    public static ResolvedParametersNEW from(List<FluentValue<?>> list, final Scope scope) {
        if(list.isEmpty()) {
            return new ResolvedParametersNEW( List.of(), scope.options() );
        }
        return new ResolvedParametersNEW( List.of(List.copyOf(list)), scope.options() );
    }


    // replace options with new options
    public ResolvedParametersNEW with(Options options) {
        return new ResolvedParametersNEW( this.pos, Objects.requireNonNull( options ) );
    }


    private ResolvedParametersNEW(List<List<FluentValue<?>>> list, Options opts) {
        this.pos = list;
        this.options = opts;
    }
    */


}
