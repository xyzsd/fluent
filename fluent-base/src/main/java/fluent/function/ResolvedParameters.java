/*
 *
 *  Copyright (C) 2021-2026, xyzsd (Zach Del)
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

package fluent.function;

import fluent.bundle.resolver.Resolver;
import fluent.bundle.resolver.Scope;
import fluent.syntax.ast.CallArguments;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/// Resolved Positional Parameters for Fluent Functions.
///
/// Typically, these originate from a CallArguments AST node.
///
@NullMarked
public sealed interface ResolvedParameters extends Iterable<FluentValue<?>> {

    // TODO: more profiling
    //       consider 'single item' just holding a single FluentValue instead of a List<FluentValue<?>>
    //       since *usually* functions are not nested

    ///  Constant: No positional arguments
    ResolvedParameters EMPTY = new Empty();

    ///  Create a ResolvedParameters from a CallArguments AST node.
    ///
    /// This only resolves positional arguments. NamedValues must be resolved via
    /// Options.from(). This will call Resolver.resolve() for each positional argument.
    static ResolvedParameters from(@Nullable CallArguments callArgs, final Scope scope) {
        requireNonNull( scope );

        // no callArgs
        if (callArgs == null) {
            return EMPTY;
        }

        return switch (callArgs.positionals().size()) {
            case 0 -> EMPTY;
            case 1 -> {
                final List<FluentValue<?>> list = Resolver.resolveExpression( callArgs.positionals().getFirst(), scope );
                yield (list.size() == 1) ? new SingleItem( list ) : new SingleList( list );
            }
            default -> new Multi( callArgs.positionals().stream()
                    .map( expression -> Resolver.resolveExpression( expression, scope ) ).toList() );

        };
    }

    ///  Create a ResolvedParameters de novo.
    ///
    /// This can be used by custom function implementations, if additional formatting via a named function
    /// is needed (and options need to be specified), and
    /// {@link fluent.bundle.FluentFunctionRegistry#implicitFormat(FluentValue, Scope)}
    /// is insufficient.
    static ResolvedParameters from(List<FluentValue<?>> pos0, final Scope scope) {
        requireNonNull( pos0 );
        requireNonNull( scope );

        // currently we support only ONE positional argument (arg0), which may have 0 or more items.
        return switch (pos0.size()) {
            case 0 -> EMPTY;
            case 1 -> new SingleItem( pos0 );
            default -> new SingleList( pos0 );
        };
    }

    ///  Create a ResolvedParameters de novo from a single FluentValue.
    static ResolvedParameters from(FluentValue<?> value, final Scope scope) {
        requireNonNull( value );
        requireNonNull( scope );

        return new SingleItem( List.of( value ) );
    }


    ///  Argument at the given position, if any.
    ///  This may return an empty list.
    default List<FluentValue<?>> positional(int index) {return List.of();}

    ///  Get the first positional argument.
    /// If there is none, throw NoSuchElementException
    List<FluentValue<?>> firstPositional() throws NoSuchElementException;

    ///  `false` if there is at least one positional argument with one value.
    default boolean isEmpty() {return false;}

    /// Stream all positional arguments, in order.
    Stream<FluentValue<?>> positionals();

    /// True if single argument with single value (not a list); just one FluentValue<?>
    default boolean isSingle() {
        return false;
    }

    /// if isSingle() == true, this returns a single FluentValue<?>; otherwise, throws a NoSuchElementException
    default FluentValue<?> singleValue() throws NoSuchElementException {
        throw new NoSuchElementException();
    }


    ///  Empty resolved parameters. Should use constant EMPTY instead of creating this de novo.
    final class Empty implements ResolvedParameters {
        private Empty() {}

        @Override
        public List<FluentValue<?>> firstPositional() throws NoSuchElementException {
            throw new NoSuchElementException();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return Stream.empty();
        }

        @Override
        public Iterator<FluentValue<?>> iterator() {
            return Collections.emptyIterator();
        }
    }

    ///  Single positional, with a list containing a single item.
    ///
    /// This is the most common type of Positional. This is a separate
    /// implementation, for stricter typing.
    record SingleItem(List<FluentValue<?>> list) implements ResolvedParameters {
        // we use a list, since we can avoid a copy for common usage (assuming
        // immutable lists are used)
        public SingleItem {
            list = List.copyOf( list );
            if (list.size() != 1) {throw new IllegalArgumentException( "Must be a single item list" );}
        }

        ///  The (single) pattern, unwrapped.
        public FluentValue<?> value() {
            return list.getFirst();
        }

        @Override
        public List<FluentValue<?>> positional(int index) {
            return (index == 0) ? list : List.of();
        }

        @Override
        public List<FluentValue<?>> firstPositional() throws NoSuchElementException {
            return list;
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return list.stream();
        }

        @Override
        public boolean isSingle() {
            return true;
        }

        @Override
        public FluentValue<?> singleValue() throws NoSuchElementException {
            return list.getFirst();
        }

        @Override
        public Iterator<FluentValue<?>> iterator() {
            return list.iterator();
        }
    }

    ///  Single positional, with a list containing multiple items.
    ///
    /// This is the second most common type of Positional.
    record SingleList(List<FluentValue<?>> list) implements ResolvedParameters {
        public SingleList {
            if (list.size() <= 1) {throw new IllegalArgumentException( "List must contain more than one item" );}
            list = List.copyOf( list );
        }

        @Override
        public List<FluentValue<?>> positional(int index) {
            return (index == 0) ? list : List.of();
        }

        @Override
        public List<FluentValue<?>> firstPositional() throws NoSuchElementException {
            return list;
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return list.stream();
        }

        @Override
        public Iterator<FluentValue<?>> iterator() {
            return list.iterator();
        }
    }

    /// Multiple positional arguments, each of which consists of a List containing one or more items.
    record Multi(List<List<FluentValue<?>>> list) implements ResolvedParameters {
        public Multi {
            requireNonNull( list );
            list = List.copyOf( list );
            assert !list.isEmpty();
        }

        @Override
        public List<FluentValue<?>> positional(int index) {
            if (index >= 0 && index < list.size()) {
                return list.get( index );
            }
            return List.of();
        }

        @Override
        public List<FluentValue<?>> firstPositional() throws NoSuchElementException {
            return list.getFirst();
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return list.stream().flatMap( List::stream );
        }

        /// Flattens contained lists (if present)
        @Override
        public Iterator<FluentValue<?>> iterator() {
            return new Iterator<>() {
                private final Iterator<List<FluentValue<?>>> outer = list.iterator();
                private Iterator<FluentValue<?>> inner = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!inner.hasNext() && outer.hasNext()) {
                        inner = outer.next().iterator();
                    }
                    return inner.hasNext();
                }

                @Override
                public FluentValue<?> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return inner.next();
                }
            };
        }
    }


}
