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
package fluent.bundle.resolver;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionCache;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.resolver.ResolutionException.CyclicException;
import fluent.function.*;
import fluent.syntax.AST.*;
import fluent.types.FluentValue;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/// State used during pattern resolution.
///
/// The lifetime of a Scope is no longer than a single FluentBundle formatPattern() call
public final class Scope {

    // bundle for this scope (immutable)
    private final FluentBundle bundle;

    // name-variable map (effectively immutable)
    private final Map<String, List<FluentValue<?>>> variableMap;

    // mutable: exceptions that have occurred during processing
    private final List<Exception> errors;

    // mutable: set of visited nodes, used as a (LIFO) stack
    private final Deque<Pattern> visited;

    // mutable: count of placeables... primarily to avoid expansion-based attacks
    private int placeables = 0;

    // mutable: named arguments for parameterized terms
    private Options termParameters = Options.EMPTY;


    // TODO: remove errors here; create a list/store exception
    ///  Scope constructor.
    public Scope(final FluentBundle bundle, final Map<String, ?> args) {
        this.bundle = bundle;
        this.errors = new ArrayList<>(4);
        this.variableMap =  remap( args );
        this.visited = new ArrayDeque<>();  // TODO: this could be via asLIFO()...
    }


    // todo: consider converting lazily/as-needed. if an argument ends up not being accessed
    //       then no conversion necessary. Should values be memoized?
    /*
        lookup : would convert
            could memoize via keeping a Hashmap in scope()
            using computeIfAbsent. does not need to be threadsafe.
     */
    /// Convert raw arguments values to FluentValues
    private static Map<String, List<FluentValue<?>>> remap(final Map<String, ?> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }

        return raw.entrySet().stream()
                .collect( Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> FluentValue.ofCollection(
                                        entry.getValue() )
                        )
                );
    }

    ///  Convenience method: Function Factory registry
    public FluentFunctionRegistry registry() {
        return bundle.registry();
    }

    ///  Convenience method: Cache
    public FluentFunctionCache cache() {
        return bundle.cache();
    }

    ///  Convenience method: Locale
    public Locale locale() {
        return bundle.locale();
    }

    ///  Convenience method: get default options for the named function
    public Options options(final String id) {
        return bundle.options(id);
    }

    ///  Convenience method: get default options, after merging
    public Options options(final String id, final Options toMerge) {
        return bundle.options(id).mergeOverriding( toMerge );
    }

    /// Used by resolution logic to prevent expansion attacks
    ///
    /// Returns false if MAX_PLACEABLES exceeded.
    public boolean incrementAndCheckPlaceables()  {
        placeables += 1;
        return (placeables > Resolver.MAX_PLACEABLES);
    }

    ///  add an exception to the list, to track errors during resolution
    public void addException(Exception t) {
        errors.add( t );
    }

    /// Errors that occurred during resolution, if any.
    public List<Exception> exceptions() {
        return List.copyOf( errors );
    }

    public boolean containsExceptions() { return !errors.isEmpty(); }

    /// fast-path: lazily add Pattern to stack, if stack is empty
    public List<FluentValue<?>> maybeTrack(final Pattern pattern, final Expression exp) {
        if (visited.isEmpty()) {
            visited.addLast( pattern );   // push
        }

        List<FluentValue<?>> result;
        try {
            result = Resolver.resolveExpression( exp, this );
        } catch (FluentFunctionException e) {
            // TODO: investigate if this is truly necessary after resolveExpression()
            return Resolver.error( e, "????" );
        }

        return result;
    }

    /// track patterns to prevent recursion
    public List<FluentValue<?>> track(final Pattern pattern, final Identifiable exp) {
        if (visited.contains( pattern )) {
            final CyclicException cyclicException = new CyclicException( exp );
            addException( cyclicException );
            return Resolver.error( cyclicException );
        } else {
            visited.addLast( pattern ); // push
            final List<FluentValue<?>> result = Resolver.resolvePattern( pattern, this );
            visited.removeLast(); // pop;
            return result;
        }
    }

    /// Used by Resolver.resolveTermReference for parameterized terms
    public void clearLocalParams() {
        termParameters = Options.EMPTY;
    }

    /// Used by Resolver.resolveTermReference for parameterized terms
    public void setLocalParams(List<NamedArgument> namedParameters) {
        termParameters = Options.from( namedParameters );
    }

    /// Lookup the given variable name (or parameterized term).
    /// This will return an empty list if the given name is not present.
    public List<FluentValue<?>> lookup(final String name) {
        // first, lookup in supplied arguments
        final List<FluentValue<?>> value = variableMap.get( name );
        if (value != null) {
            return value;
        }

        // Could be a parameterized term.
        // We then look up named-pair Options, which are never in Lists.
        // These are also not FluentValues, and must be converted
        return termParameters.asRaw( name )
                .map( FluentValue::of )
                .<List<FluentValue<?>>>map( List::of )
                .orElse( List.of() );
    }

    // CallArguments are merged with scope options; call args options override initial scope options
    public ResolvedParameters resolveParameters(@Nullable final CallArguments callArgs) {
        return ResolvedParameters.from( callArgs, this );
    }


    public FluentBundle bundle() {
        return bundle;
    }


}
