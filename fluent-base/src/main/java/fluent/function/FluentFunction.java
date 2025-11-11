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
package fluent.function;

import fluent.bundle.resolver.Scope;
import fluent.syntax.ast.VariantKey;
import fluent.types.FluentError;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

///
/// All Fluent Functions, both implicit and explicit, must implement
/// at least one subtype of this interface.
///
@NullMarked
public sealed interface FluentFunction {


    /// Throw an exception if input is empty (zero length).
    /// In general, most FluentFunctions expect input of at least one item.
    ///
    /// If empty input is acceptable, do not use this utility method.
    ///
    /// @param rp ResolvedParameters
    static void ensureInput(final ResolvedParameters rp) {
        if (rp.isEmpty()) {
            throw FluentFunctionException.of( "No positional arguments supplied; at least one is required." );
        }
    }

    /// Apply the given function to the input pattern if and only if it is assignable to the given class type.
    ///
    /// This is best used with {@link Stream#mapMulti(BiConsumer)}.
    ///
    /// FluentErrors are always passed through.
    ///
    /// Types that are not assignable to `Class<T>` will also be passed through.
    static <T> BiConsumer<? super FluentValue<?>, ? super Consumer<FluentValue<?>>> mapOrPassthrough(Class<T> cls, Function<T, ?> fn) {
        return (in, consumer) -> {
            // specifically reference FluentError here, just in case we are operating on String.
            // We do not want FluentErrors to be modified.
            if (in instanceof FluentError) {
                // passthrough errors
                consumer.accept( in );
            } else if (cls.isInstance( in.value() )) {
                final T value = cls.cast( in.value() );
                consumer.accept( FluentValue.of( fn.apply( value ) ) );
            } else {
                // passthrough non-matching
                consumer.accept( in );
            }
        };
    }

    ///  Apply the given function to the input pattern if and only if it is a FluentNumber.
    static BiConsumer<? super FluentValue<?>, ? super Consumer<FluentValue<?>>>
    mapFluentNumberOrPassthrough(final Function<FluentNumber<?>, ?> fn) {
        return (in, consumer) -> {
            if (in instanceof FluentNumber<?> fluentNumber) {
                consumer.accept( FluentValue.of( fn.apply( fluentNumber ) ) );
            } else {
                // passthrough
                consumer.accept( in );
            }
        };
    }

    ///  Apply the given function to the input pattern if and only if it is a FluentNumber.
    static BiConsumer<? super FluentValue<?>, ? super Consumer<FluentValue<?>>>
    mapFluentNumberOrError(final Function<FluentNumber<?>, ?> fn) {
        return (in, consumer) -> {
            if (in instanceof FluentNumber<?> fluentNumber) {
                consumer.accept( FluentValue.of( fn.apply( fluentNumber ) ) );
            } else {
                // error
                throw FluentFunctionException.of( "Expected FluentNumber<?>; instead, '%s:%s'",
                        in.getClass().getSimpleName(), in.value() );
            }
        };
    }

    /// Apply the given function to the input pattern if and only if it is assignable to the given class type.
    ///
    /// This is best used with {@link Stream#mapMulti(BiConsumer)}.
    ///
    /// FluentErrors are always passed through.
    ///
    /// Types that are not assignable to `Class<T>` will be *discarded*.
    static <T> BiConsumer<? super FluentValue<?>, ? super Consumer<FluentValue<?>>> mapOrDiscard(Class<T> cls, Function<T, ?> fn) {
        return (in, consumer) -> {
            if (in instanceof FluentError) {
                consumer.accept( in );
            } else if (cls.isInstance( in.value() )) {
                final T value = cls.cast( in.value() );
                consumer.accept( FluentValue.of( fn.apply( value ) ) );
            }
        };
    }

    /// Throw an exception if the given pattern is a FluentError.
    /// Otherwise, do nothing.
    ///
    /// @param in FluentValue to check
    static FluentValue<?> validate(final FluentValue<?> in) {
        if (in instanceof FluentError(String value)) {
            throw FluentFunctionException.of( value );
        }
        return in;
    }

    /// Create a FluentFunction.Transform which is applied to all input matching the class type.
    /// Empty input is not allowed.
    static <T> Transform passthroughTransform(Class<T> cls, Function<T, ?> fn) {
        final var biConsumer = mapOrPassthrough( cls, fn );
        return (param, __) -> {
            ensureInput( param );
            return param.positionals().mapMulti( biConsumer ).toList();
        };
    }

    /// Transform: transform parameters
    ///
    /// Transform functions convert parameters into other [FluentValue]s, and these [FluentValue]s
    /// need not be [FluentString]s (unlike [Formatter]s)
    non-sealed interface Transform extends FluentFunction {
        ///  Apply the transform.
        List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope);
    }


    /// Formatter: Format a single [FluentValue] to a [FluentString]
    ///
    /// Note: Formatters are also Transforms and Selectors.
    ///
    /// *Implementation Note:* the [Transform#apply] for Formatters *must* only return
    /// [FluentString]s or [FluentError]s; no other values should be returned.
    interface Formatter<T> extends Transform, Selector {
        ///  Format the value to a FluentString or FluentError
        FluentValue<String> format(FluentValue<? extends T> in, Scope scope);
    }


    /// Selector: implements selection functionality.
    ///
    /// Selector functionality can be different from transform functionality.
    /// For example, by default, the [`NUMBER`][fluent.function.functions.numeric.NumberFn] function selects into
    /// [CLDR plural categories](https://cldr.unicode.org/index/cldr-spec/plural-rules).
    ///
    /// Most selectors are designed to operate on a single `FluentValue`, and cannot operate on multiple arguments or lists.
    /// [VariantKey]s are typically matched by exact String comparison. However, this method exposes the `VariantKey`s
    /// (which can be String or Number literals) and allows multiple arguments, to allow better custom matching as needed.
    ///
    non-sealed interface Selector extends FluentFunction {

        /// Utility method: if ResolvedParameters consists of a single item, return it.
        /// Otherwise, throw an exception detailing why selection cannot be performed.
        static FluentValue<?> ensureSingle(final ResolvedParameters parameters) {
            if (parameters instanceof ResolvedParameters.SingleItem singleItem) {
                return singleItem.value();
            } else if (parameters instanceof ResolvedParameters.Empty) {
                throw FluentFunctionException.of( "No argument on which to select." );
            } else {
                throw FluentFunctionException.of( "Cannot select() on multiple items or lists: '[%s]' ", parameters );
            }
        }

        /// Select
        VariantKey select(ResolvedParameters parameters, List<VariantKey> variantKeys, VariantKey defaultKey, Scope scope);

    }

    ///
    /// Reduce a value (or multiple values) to a single (String) value.
    /// This is (by default) the LIST formatter, which is a terminal operation.
    ///
    /// Only one `TerminalReducer` implementation is allowed per FluentBundle.
    ///
    /// A TerminalReducer is ALWAYS invoked during formatting unless there is an error.
    ///
    /// Reducers MUST reduce values to a single FluentString. So the TerminalReducer.Transform implementation
    /// must reduce to a single-item `List<FluentValue<String>>`.
    ///
    /// The apply() method must also be implemented, and must return
    /// a `List<FluentValue<String>>`.
    ///
    /// The Reducer interface is used for implicit formatting (e.g., `{ $listOfItems }`).
    /// The Transform function is used for the explicit form (e.g., `{ LIST($listOfItems)}`).
    ///
    interface TerminalReducer extends Transform {
        ///
        /// Format one or more FluentValues to a single String pattern.
        ///
        /// This is a terminal operation.
        ///
        /// @param in    pattern(s) to format
        /// @param scope Scope
        /// @return formatted pattern(s) as a String
        String reduce(List<FluentValue<?>> in, Scope scope);

        // TODO: apply() must return List<FluentValue<String>>
        // ...
    }


}
