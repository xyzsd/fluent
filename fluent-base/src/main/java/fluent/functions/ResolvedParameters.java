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

import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Resolved Parameters for Fluent Functions.
 */
public class ResolvedParameters {

    //      TODO: look into performance.
    //          in many cases, there is but a single argument [possibly with options]
    //          perhaps have special subclass for that, and eliminate the 'list of lists..'
    //          with interface being public; subclasses would be package-protected or private
    //          ...
    //          another option could be just a single list, and then
    //          most of the time positional arguments are just iterated through in-order
    //          track offsets into that list for Positionals (could keep an int[] array.... etc.)
    //          since typically valuesAll() is used rather than indexing specific positionals
    //          could then use sublist to get specific positionals as needed. Also would be easier on
    //          creation to filter/flatten lists (e.g., remove empty lists)
    //          ...
    //          or, perhaps just handle everything as Streams instead. This approach may be better,
    //          though it would no longer (without substantial effort) be possible to address
    //          arguments (positionals) individually (since they would be merged into a single stream).
    //          This may not be a problem in practice but may potentially limit the expressivity of
    //          functions, for example.


    /**
     * Empty
     */
    public static final ResolvedParameters EMPTY = new ResolvedParameters( List.of(), Options.EMPTY );


    /**
     * Positional argument index
     */
    public enum Positional {
        // Enum order is particularly meaningful here
        FIRST, SECOND, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH, EIGHTH, NINTH, TENTH
    }

    private static final int MAX_POSITIONALS = Positional.values().length;

    // positional arguments. single-items are in single-item lists (via List.of())
    // e.g. FUNCTION("hello", "goodbye") :
    //      "hello" would be the first positional, and would be a single item list
    //      "goodbye" is the second positional, and would also be a single item list
    // so this would be List( List(FluentString("hello")), List(FluentString("goodbye")) )
    private final List<List<FluentValue<?>>> pos;

    // Options are the key-value pairs
    private final Options options;


    // could use a builder instead....
    public static ResolvedParameters from(final List<List<FluentValue<?>>> listIn, final Options opts) {
        Objects.requireNonNull( listIn );
        Objects.requireNonNull( opts );

        // remove empty lists
        final List<List<FluentValue<?>>> collapsed = listIn.stream()
                .filter( Predicate.not( List::isEmpty ) )
                .toList();

        // note: this is somewhat arbitrary...
        if(collapsed.size() >= MAX_POSITIONALS) {
            throw new IllegalArgumentException(MAX_POSITIONALS+" arguments maximum!");
        }

        return new ResolvedParameters( collapsed, opts );
    }


    // typically used by FluentValue<> formatters
    // uses options supplied in scope -- if any
    public static ResolvedParameters from(FluentValue<?> value, final Scope scope) {
        return new ResolvedParameters( List.of(List.of(value)), scope.options() );
    }

    public static ResolvedParameters from(List<FluentValue<?>> list, final Scope scope) {
        if(list.isEmpty()) {
            return new ResolvedParameters( List.of(), scope.options() );
        }
        return new ResolvedParameters( List.of(List.copyOf(list)), scope.options() );
    }


    // replace options with new options
    public ResolvedParameters with(Options options) {
        return new ResolvedParameters( this.pos, Objects.requireNonNull( options ) );
    }


    private ResolvedParameters(List<List<FluentValue<?>>> list, Options opts) {
        this.pos = list;
        this.options = opts;
    }


    public Options options() {
        return options;
    }


    // returns a single value (first item from list) or empty
    public Optional<FluentValue<?>> valueFirst(final Positional p) {
        final int index = p.ordinal();
        if (index < pos.size()) {
            return Optional.of( pos.get( index ).get( 0 ) );
        }
        return Optional.empty();
    }

    // returns a stream (of 0 or more elements) for a given position
    public Stream<FluentValue<?>> valueStream(final Positional p) {
        final int index = p.ordinal();
        if (index < pos.size()) {
            return pos.get( index ).stream();
        }
        return Stream.empty();
    }

    // stream ALL arguments, in order
    public Stream<FluentValue<?>> valuesAll() {
        return pos.stream().flatMap( List::stream );
    }


    public int valueCount(final Positional p) {
        final int index = p.ordinal();
        if (index < pos.size()) {
            return pos.get( index ).size();
        }
        return 0;
    }


    /**
     * Count the number positional arguments.
     */
    public int positionalCount() {
        return pos.size();
    }

    public boolean noPositionals() {
        return pos.isEmpty();
    }

    public boolean hasPositional(final Positional p) {
        return (p.ordinal() < pos.size());
    }


    @Override
    public String toString() {
        return (this == EMPTY)
                ? "ResolvedParameters{ResolvedParameters.EMPTY}"
                : ("ResolvedParameters{" + "pos=" + pos + ", options=" + options + '}');
    }
}
