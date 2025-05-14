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
import fluent.syntax.AST.SelectExpression;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentError;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * Implementation of Fluent Functions.
 *
 *
 * <p>
 * Functions should:
 * <ul>
 *      <li>be pure (deterministic and free from side effects)</li>
 *      <li>be threadsafe (easy if pure, and no data is cached)</li>
 *      <li>output at least one value, or fail with a {@code FluentFunctionException}</li>
 * </ul>
 * <p>
 *     General recommendations:
 *     <ul>
 *         <li>
 *             Functions will generally compose better if they are permissive with data input;
 *             e.g., a Number formatting function should pass-through unchanged
 *             non-FluentNumber values it sees. However, this approach may not make sense for all functions.
 *         </li>
 *         <li>
 *             Functions should ignore options (specifically, option names) that have been set but are
 *             not known to the function. This makes it easier to set options that could apply to many
 *             functions globally.
 *         </li>
 *         <li>
 *              Functions should have a static String constant denoting the function name (preferably called NAME).
 *              This name should also be returned by the name() method.
 *         </li>
 *     </ul>
 * <p>
 *     By convention, implementation class names have an "Fn" suffix
 *     (e.g., the class for the fluent function NUMBER is called NumberFn).
 * </p>
 * <p>
 *      Functions should explicitly fail if there is no input, and input is required.
 * </p>
 * <p>
 *     Characters permitted in function names: {@code [A-Z], [0-9], '-' and '_'}, per the Fluent specification.
 * </p>
 */
public interface FluentFunction {

    /**
     * Fluent Function name (per Fluent Function name guidelines)
     */
    String name();


    /**
     * Function application.
     * <p>
     * Functions should never return zero-length lists (or null), but
     * instead throw a FluentFunctionException if they fail.
     * </p>
     *
     * @param parameters input
     * @param scope      scope
     * @return Output list, of at least one item.
     */
    List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope);


    /**
     * Function application specific to select blocks.
     * <p>
     * Some functions may wish to exhibit different behavior for select blocks instead of using
     * the apply() behavior. Generally this method does not need to be overridden, but functions
     * except for functions that reduce their input.
     * </p>
     * <p>
     * Functions should never return zero-length lists (or null), but
     * instead throw a FluentFunctionException if they fail.
     * </p>
     */
    default List<FluentValue<?>> select(final SelectExpression selectExpression,
                                        final ResolvedParameters parameters,
                                        final Scope scope) {
        // this should work for most functions ... unless they reduce!
        return apply( parameters, scope ).stream()
                .map( v -> v.select( selectExpression, parameters, scope ) )
                .map( v -> Resolver.resolve( v.value(), scope ) )
                .flatMap( List::stream )
                .toList();
    }


    /**
     * Throw an exception if input is empty (zero length).
     * In general, most FluentFunctions expect input of at least one item.
     * <p>
     * If empty input is acceptable, do not use this utility method.
     * </p>
     *
     * @param rp ResolvedParameters
     */
    static void ensureInput(final ResolvedParameters rp) {
        if (rp.noPositionals()) {
            throw FluentFunctionException.create( "No positional arguments supplied; at least one is required." );
        }
    }


    /**
     * Utility method: get Number from FluentValue (FluentNumber) or throw an exception.
     *
     * @param input input
     * @return Number (if input FluentValue is a FluentNumber)
     * @throws FluentFunctionException if input is not a FluentNumber
     */
    static Number asNumber(final FluentValue<?> input) {
        return ((FluentNumber<?>) asFluentValue( FluentNumber.class, input )).value();
    }


    /**
     * Utility method: get String from FluentValue (FluentNumber) or throw an exception.
     *
     * @param input input
     * @return Number (if input FluentValue is a FluentString)
     * @throws FluentFunctionException if input is not a FluentString
     */
    static String asString(final FluentValue<?> input) {
        return asFluentValue( FluentString.class, input ).value();
    }


    /**
     * Utility method: get specific FluentValue subclass from FluentValue or throw an exception.
     *
     * @param type  Required FluentValue type
     * @param input FluentValue input
     * @param <T>   (FluentValue subtype)
     * @return FluentValue as requested Type
     * @throws FluentFunctionException if input is not of {@code type}
     */
    static <T extends FluentValue<?>> T asFluentValue(final Class<T> type,
                                                      final FluentValue<?> input) {
        if (type.isInstance( input )) {
            return type.cast( input );
        }

        throw FluentFunctionException.create(
                "Positional argument '%s': expected type '%s', not '%s'",
                String.valueOf( input ),
                type.getSimpleName(),
                input.getClass().getSimpleName()
        );
    }


    /**
     * Apply the given function to the input value 'in' if and only if 'in' is a FluentNumber.
     * <p>
     * The number function can return FluentValues or a non-FluentValue type; if
     * it returns a non-FluentValue, it will be wrapped with the appropriate
     * FluentValue type (e.g., Numbers -> FluentNumber, String -> FluentString, etc.)
     * </p>
     * <p>
     * If input is NOT a FluentNumber, the function is NOT applied, and no exception is
     * generated, UNLESS a FluentError is encountered. If a FluentError is the input type,
     * an exception is thrown.
     * </p>
     *
     * @param in    input value
     * @param scope Scope
     * @param fn    function to aply
     * @return value after applying fn
     * @throws FluentFunctionException if FluentError encountered in input
     */
    // TODO: generify for non-numbers
    static FluentValue<?> applyIfNumber(final FluentValue<?> in, final Scope scope, final Function<Number, ?> fn) {
        validate( in );
        if (in.value() instanceof Number num) {
            return FluentValue.toFluentValue( num );
        } else {
            return in;
        }
    }


    /**
     * Sugared version of applyIfNumber() for Streams.
     */
    static List<FluentValue<?>> mapOverNumbers(final Stream<FluentValue<?>> stream,
                                               final Scope scope,
                                               final Function<Number, ?> fn) {
        return stream.<FluentValue<?>>map(
                value -> FluentFunction.applyIfNumber( value, scope, fn )
                ).toList();
    }



    /**
     * Throw an exception if the given value is a FluentError.
     * Otherwise, do nothing.
     *
     * @param in FluentValue to check
     */
    static void validate(FluentValue<?> in) {
        if (in instanceof FluentError error) {
            throw FluentFunctionException.create( error.value() );
        }
    }

}
