package fluent.functions;

import fluent.bundle.resolver.Scope;
import fluent.syntax.AST.SelectExpression;
import fluent.types.FluentError;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/// Fluent Function Interface.
///
/// Functions should:
///
///    - be pure (deterministic and free from side effects)
///    - be threadsafe (easy if pure, and no data is cached)
///    - output at least one value, or fail with a `FluentFunctionException`
///
///
///     General recommendations:
///
///       - Functions will generally compose better if they are permissive with data input;
///     e.g., a Number formatting function should pass-through unchanged
///     non-FluentNumber values it sees. However, this approach may not make sense for all functions.
///
///       - Functions should ignore options (specifically, option names) that have been set but are
///     not known to the function. This makes it easier to set options that could apply to many
///     functions globally.
///
///       - Functions should have a static String constant denoting the function name (preferably called NAME).
///     This name should also be returned by the name() method.
///
///
///
///     By convention, implementation class names have an "Fn" suffix
///     (e.g., the class for the fluent function NUMBER is called NumberFn).
///
///
///      Functions should explicitly fail if there is no input, and input is required.
///
///
///     Characters permitted in function names: `[A-Z],[0-9], '-' and '_'`, per the Fluent specification.
///
///
/// This class also has static utility methods useful for implementing functions.
///
///
@NullMarked
public interface FluentFunction {


    /// Throw an exception if input is empty (zero length).
    /// In general, most FluentFunctions expect input of at least one item.
    ///
    /// If empty input is acceptable, do not use this utility method.
    ///
    /// @param rp ResolvedParameters
    static void ensureInput(final ResolvedParameters rp) {
        if (!rp.hasPositionals()) {
            throw FluentFunctionException.create( "No positional arguments supplied; at least one is required." );
        }
    }


    /// Apply the given function to the input value if and only if it is assignable to the given class type.
    ///
    /// This is best used with {@link Stream#mapMulti(BiConsumer)}.
    ///
    /// FluentErrors are always passed through.
    ///
    /// Types that are not assignable to `Class<T>` will also be passed through.
    static <T> BiConsumer<? super FluentValue<?>, ? super Consumer<FluentValue<?>>> mapOrPassthrough(Class<T> cls, Function<T, ?> fn) {
        return (in, consumer) -> {
            // specific FluentError here, just in case we are operating on String.
            // We do not want FluentErrors to be modified.
            if (!(in instanceof FluentError) && cls.isInstance( in.value() )) {
                final T value = cls.cast( in.value() );
                consumer.accept( FluentValue.toFluentValue( fn.apply( value ) ) );
            } else {
                // passthrough
                consumer.accept( in );
            }
        };
    }

    /// Apply the given function to the input value if and only if it is assignable to the given class type.
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
                consumer.accept( FluentValue.toFluentValue( fn.apply( value ) ) );
            }
        };
    }


    /// Fluent Function name (per Fluent Function name guidelines)
    String name();

    /// Function application.
    ///
    /// Functions should never return zero-length lists (or null). Functions may return
    /// a different object--or type of object--than what was input.
    ///
    /// A FluentFunctionException should be thrown if there is an error, but
    /// a FluentError can be returned to preferably salvage possible message context.
    ///
    /// @param parameters input
    /// @param scope      scope
    /// @return FluentObject
    default List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) throws FluentFunctionException {
        return List.of( new FluentError( String.format( "%s(): cannot apply().", name() ) ) );
    }

    /// Function application specific to select blocks.
    ///
    /// This will return a FluentError *unless* overridden. Functions may wish to exhibit
    /// different behavior than {@link #apply(ResolvedParameters, Scope)} for select blocks.
    ///
    /// @param selectExpression select expression
    /// @param parameters       input
    /// @param scope            scope
    /// @return Output list, of at least one item.
    default FluentValue<?> select(final SelectExpression selectExpression,
                                  final ResolvedParameters parameters,
                                  final Scope scope) throws FluentFunctionException {
        return new FluentError( String.format( "%s(): cannot select().", name() ) );
    }

}
