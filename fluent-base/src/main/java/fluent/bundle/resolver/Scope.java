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

package fluent.bundle.resolver;

import fluent.bundle.FluentBundle;
import fluent.functions.*;
import fluent.syntax.AST.CallArguments;
import fluent.syntax.AST.Expression;
import fluent.syntax.AST.Identifiable;
import fluent.syntax.AST.Pattern;
import fluent.types.FluentValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mutable state used during pattern resolution.
 *
 * The lifetime of a Scope is no longer than a single formatPattern() call
 * from the MessageBundle
 */
public class Scope {

    /**
     *  Map variable names to FluentValues
     *  <p>
     *      NOTE: The (internal) use of this interface is not strictly necessary but this allows
     *      future flexibility and the possibility of more easily re-scoping variable lookups depending
     *      upon the resolution context.
     *  </p>
     * */
    public interface ValueMapper {
        /** Return a FluentValue for a name. If not found, return an empty list. */
        List<FluentValue<?>> lookup(String name);

        /** Default ValueMapper for Maps */
        static ValueMapper from(Map<String, List<FluentValue<?>>> map) {
            return map::get;
        }
    }

    // bundle for this scope (immutable)
    private final FluentBundle bundle;

    // function resources (immutable)
    public final FunctionResources fnResources;

    // developer-set options valid for entire life of this Scope (immutable)
    // never null
    @NotNull private final Options options;

    // name-variable map (immutable)
    private final ValueMapper valueMapper;

    // mutable: exceptions that have occurred during processing
    private final List<Exception> errors;

    // mutable: set of visited nodes, used as a (LIFO) stack
    // todo: this could be via asLIFO()...
    private final Deque<Pattern> visited;

    // mutable: count of placeables... primarily to avoid expansion-based attacks
    private int placeables = 0;

    // mutable: halt further processing if true
    private boolean isDirty;

    // mutable: local arguments
    private ResolvedParameters localParams = ResolvedParameters.EMPTY;




    ///////////////////////////

    public Scope(FluentBundle bundle, FunctionResources res, Map<String, ?> args, List<Exception> errors) {
        this( bundle, res, args, errors, Options.EMPTY );
    }

    // with Options
    public Scope(FluentBundle bundle, FunctionResources res, Map<String, ?> args, List<Exception> errors,
                 @NotNull Options options) {
        this.bundle = bundle;
        this.fnResources = res;
        this.errors = errors;
        this.valueMapper = ValueMapper.from( remap( args ) );
        this.visited = new ArrayDeque<>();
        this.options = options;
    }


    // private, for rescoping
    private Scope(final Scope from, ValueMapper mapper) {
        this.bundle = from.bundle;
        this.errors = from.errors;
        this.options = from.options;
        this.fnResources = from.fnResources;

        this.valueMapper = mapper;

        // reset visited...
        this.visited = new ArrayDeque<>();
    }


    // NOTE: this is a work in progress and is not currently used
    // this *may* be used in select clauses for lists, to capture the current item from the list
    public Scope rescope(@NotNull ValueMapper vm) {
        // this also has a new 'visited' stack
        return new Scope(this, vm);
    }


    // Convert raw values to FluentValues
    private Map<String, List<FluentValue<?>>> remap(@NotNull final Map<String, ?> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }

        return raw.entrySet().stream()
                .collect( Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> bundle.valueCreator()
                                .toCollection( entry.getValue() )
                        )
                );
    }


    /** Options */
    public Options options() {
        return options;
    }

    /**
     * Used by resolution logic.
     */
    public void incrementAndCheckPlaceables() {
        placeables += 1;
        if (placeables > Resolvable.MAX_PLACEABLES) {
            isDirty = true;
            throw new ResolutionException( "Too many placeables" );
        }
    }


    public boolean isDirty() {
        return isDirty;
    }

    // add an exception TODO: should rename to 'addException'
    public void addError(@NotNull RuntimeException t) {
        errors.add( t );
    }

    // this is NOT a copy; should only be used when Scope no longer is needed for resolution
    // todo: should rename to 'exceptions'
    public List<Exception> errors() {
        return errors;
    }


    // fast-path: lazily add Pattern to stack, if stack is empty
    public List<FluentValue<?>> maybeTrack(final Pattern pattern, final Expression exp) {
        if (visited.isEmpty()) {
            visited.addLast( pattern );   // push
        }

        List<FluentValue<?>> result;
        try {
            result = exp.resolve( this );
        } catch (FluentFunctionException e) {
            return Resolvable.error( e.fnName().orElse( "???" ) + "()" );
        }

        if (isDirty) {
            addError( new ResolutionException( exp.toString() ) );
            return Resolvable.error( "{!dirty: " + exp + "}" );
        }

        return result;
    }


    public List<FluentValue<?>> track(final Pattern pattern, final Identifiable exp) {
        if (visited.contains( pattern )) {
            addError( new ResolutionException( "Cyclic" ) );
            return Resolvable.error( "{!cyclic:" + exp.name() + "}" );
        } else {
            visited.addLast( pattern ); // push
            final List<FluentValue<?>> result = pattern.resolve( this );
            visited.removeLast(); // pop;
            return result;
        }
    }


    public void clearLocalParams() {
        localParams = ResolvedParameters.EMPTY;
    }


    public void setLocalParams(@Nullable final CallArguments args) {
        localParams = resolveParameters( args );
    }


    public ResolvedParameters getLocalParams() {
        return localParams;
    }


    // empty list if value not present
    public List<FluentValue<?>> lookup(@NotNull final String name) {
        // first, lookup in supplied arguments
        final List<FluentValue<?>> value = valueMapper.lookup( name );
        if(value != null) {
            return value;
        }

        // then lookup in named-pair Options, which are never in Lists.
        // these are also not FluentValues, and must be converted
        return localParams.options().asRaw( name )
                .map( bundle.valueCreator()::toFluentValue )
                .<List<FluentValue<?>>>map( List::of )
                .orElse( List.of() );
    }

    // CallArguments are merged with scope options; call args options override initial scope options
    public ResolvedParameters resolveParameters(@Nullable final CallArguments callArgs) {
        final List<List<FluentValue<?>>> rezPoz = (callArgs == null)
                ? List.of()
                : callArgs.positional().stream().map( e -> e.resolve( this ) ).toList();

        final Options opts = options.mergeOverriding( Options.from( callArgs ) );

        return ResolvedParameters.from( rezPoz, opts );
    }


    public FluentBundle bundle() {
        return bundle;
    }


    /**
     * Call the (Implicit) reduction function "JOIN()"
     */
    public String reduce(final List<FluentValue<?>> in) {
        return ((ImplicitReducer) bundle.implicit( FluentImplicit.Implicit.JOIN )).reduce( in, this );
    }
}
