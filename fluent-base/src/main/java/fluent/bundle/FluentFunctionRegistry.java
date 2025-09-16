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
package fluent.bundle;

import fluent.bundle.resolver.Scope;
import fluent.function.*;
import fluent.function.functions.list.reducer.ListFn;
import fluent.function.functions.numeric.NumberFn;
import fluent.function.functions.temporal.DateTimeFn;
import fluent.syntax.AST.SelectExpression;
import fluent.syntax.AST.VariantKey;
import fluent.types.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

///
/// Registry of Fluent Functions (FluentFunctions)
///
/// This class maintains an immutable, threadsafe function registry, and handles both implicit and explicit functions.
///
/// The registry is not Locale-dependent and can be shared between FluentBundles.
@NullMarked
public final class FluentFunctionRegistry {

    // REQUIRED IMPLICIT FORMATTERS:
    // These are invoked automatically based on the FluentValue type.
    //
    // Note that it is possible (though not likely desirable!) for the implicit function and the explicit
    // function to be different.
    //
    // The implicit forms are also created during initializations.
    //
    // (1) FluentStrings and FluentErrors: converted to their contained String values automatically.
    //     automatic; no function factory required
    // (2) Numbers/Plurals:
    private final FluentFunctionFactory<FluentFunction.Formatter<Number>> numberFmtFactory;
    // (3) Temporal values (dates/times):
    private final FluentFunctionFactory<FluentFunction.Formatter<TemporalAccessor>> temporalFmtFactory;

    // REQUIRED IMPLICIT REDUCER
    // because we support Lists automatically (*except* in select statements), we need a function that
    // will reduce a list to a single-item list containing a FluentString. That is the 'reducer'.
    private final FluentFunctionFactory<FluentFunction.TerminalReducer> reducerFmtFactory;

    // FUNCTION FACTORIES
    // BY NAME: (function called explicitly, e.g., { NUMBER($var) }
    private final Map<String, FluentFunctionFactory<?>> functionNameMap;

    // CUSTOM IMPLICIT TYPES:
    // Typically functions must be called by name, except for the above-defined implicit formatters.
    // However, formatters for custom types (e.g., FluentCustom<Boolean>) can be defined.
    // We permit two ways of matching types and implicit formatters: 'exact' and 'subtype'
    // if there is NO match, a 'default' custom formatter (String.valueOf()) will be used.

    // CUSTOM TYPES: EXACT MATCH:
    // If a class matches exactly (not a subtype), the exact match is used. This is evaluated first.
    private final Map<Class<?>, C2FEntry<?>> customExact;

    // CUSTOM TYPES: SUBTYPE MATCH:
    // if a class fulfills 'isInstance' (so, that type or any subtype), use that formatter.
    // This is evaluated after checking for an exact match.
    private final List<C2FEntry<?>> customList;


    private FluentFunctionRegistry(final Builder builder) {
        // create required implicit (default) formatters. Already null-checked.
        this.numberFmtFactory = builder.numberFactory;
        this.temporalFmtFactory = builder.temporalFactory;
        this.reducerFmtFactory = builder.reducerFactory;
        this.functionNameMap = Map.copyOf( builder.factories );
        this.customExact = Map.copyOf( builder.exact );
        this.customList = List.copyOf( builder.subtypes.sequencedValues() );
    }

    /// Get a (non-threadsafe!) Builder to create a FluentFunctionRegistry.
    public static Builder builder() {
        return new Builder();
    }

    ///  True if function with this name exists in the registry.
    public boolean contains(final String functionName) {
        return functionNameMap.containsKey( functionName );
    }


    /// Basic Function lookup.
    ///
    /// This method will return null if `functionName` is missing or not of the correct type.
    /// (use `FluentFunction.class` for 'any' `FluentFunction`)
    ///
    /// *NOTE: This does NOT modify passed-in namedParameters (`Options`)*; in other words,
    /// named parameters are passed 'as-is' and default options (if any) are not applied.
    ///
    /// @throws FluentFunctionException if factory function creation fails
    public @Nullable <T extends FluentFunction> T getFunction(final String functionName,
                                                              final Class<T> type,
                                                              final FluentFunctionCache cache,
                                                              final Locale locale,
                                                              final Options namedParameters) {
        requireNonNull( functionName );
        requireNonNull( type );
        requireNonNull( cache );
        requireNonNull( locale );
        requireNonNull( namedParameters );

        final FluentFunctionFactory<?> factory = functionNameMap.get( functionName );
        if (factory != null) {
            final FluentFunction function = cache.getFunction( factory, locale, namedParameters );

            if (type.isInstance( function )) {
                return type.cast( function );
            }
        }
        return null;
    }

    // true if any custom formatters have been set
    private boolean hasCustoms() {
        return !customExact.isEmpty() && !customList.isEmpty();
    }

    /// Select on a FluentValue
    ///
    /// This method is used for default (implicit) selection.
    ///
    /// Note that selection does NOT work (an exception will occur) if the list of values is either empty or
    /// greater than one.
    ///
    /// Implicit selection occurs when no function is called as a selector, *or*
    /// the called function does not implement select. If a called function does NOT
    /// implement select, the output of that function becomes the input to [#implicitSelect]
    ///
    /// *NOTE:* unlike [#implicitFormat(FluentValue, Scope)], this method CAN throw [FluentFunctionException]s.
    ///
    /// @param values Input on which we select
    /// @param exp    SelectExpression
    /// @param scope  Scope
    /// @return VariantKey of the given SelectExpression.
    public VariantKey implicitSelect(final List<FluentValue<?>> values, final SelectExpression exp, final Scope scope) {
        if (values.size() != 1) {
            throw FluentFunctionException.of( "Cannot select on a empty function result or on multiple values [%d:%s]!",
                    values.size(), values );
        }

        final FluentValue<?> fluentValue = values.getFirst();

        return switch (fluentValue) {
            // Select variant that exactly matches this FluentString, if any
            case FluentString(String value) -> exp.matchOrDefault( value ).key();

            // errors: always return default key
            case FluentError _ -> exp.defaultVariantKey();

            case FluentNumber<? extends Number> fluentNumber -> selectNumber( fluentNumber, exp, scope );
            case FluentTemporal fluentTemporal -> selectTemporal( fluentTemporal, exp, scope );
            case FluentCustom<?> custom -> selectCustom( custom, exp, scope );
        };
    }

    /// Formats a FluentValue to a `FluentValue<String>`
    ///
    /// This method is used for implicit (default) formatting
    /// (when no function is named)
    ///
    /// This method will not throw FluentFunctionExceptions.
    ///
    /// @param fluentValue Value which we are formatting
    /// @param scope       Scope
    /// @return formatted String or brief error message as a String. Never null.
    public String implicitFormat(FluentValue<?> fluentValue, Scope scope) {
        try {
            return switch (fluentValue) {
                // do nothing for String/Error values.
                case FluentString(String s) -> s;
                case FluentError(String e) -> e;

                // build-in default (implicit) formatters
                case FluentNumber<? extends Number> fluentNumber -> formatNumber( fluentNumber, scope );
                case FluentTemporal fluentTemporal -> formatTemporal( fluentTemporal, scope );

                // defined custom formatters
                case FluentCustom<?> custom when hasCustoms() -> formatCustom( custom, scope );

                // failsafe custom formatter (in case no custom formatter is defined)
                case FluentCustom(var value) -> String.valueOf( value );
            };
        } catch (FluentFunctionException e) {
            scope.addException( e );
            return "{[implicitFormat] FluentFunctionException}";
        }
    }

    ///  default (implicit) reducer (no arguments supplied)
    ///
    ///  This will always reduce, and will *not* throw a FluentFunctionException.
    ///  If an error occurs, an error message will be output and scope updated with
    ///  error information as appropriate.
    public String reduce(final List<FluentValue<?>> in, final Scope scope) {
        // short-circuit common case for performance (implicit list, size 1)
        if (in.size() == 1 && in.getFirst() instanceof FluentString(String value)) {
            return value;
        }

        try {
            return scope.cache().getFunction( reducerFmtFactory, scope.locale(), scope.options( reducerFmtFactory.name() ) )
                    .reduce( in, scope );
        } catch (FluentFunctionException e) {
            scope.addException( e );
            return "{[implicit " + reducerFmtFactory.name() + " FluentFunctionException}";
        }
    }

    ///  implicit number format
    private String formatNumber(final FluentNumber<? extends Number> fluentNumber, final Scope scope) {
        return scope.cache().getFunction( numberFmtFactory, scope.locale(), scope.options( numberFmtFactory.name() ) )
                .format( fluentNumber, scope )
                .value();
    }

    ///  implicit number select (plural/cardinal is typical)
    private VariantKey selectNumber(final FluentNumber<? extends Number> fluentNumber, final SelectExpression exp, final Scope scope) {
        final ResolvedParameters singleParam = ResolvedParameters.from( fluentNumber, scope );
        return scope.cache().getFunction( numberFmtFactory, scope.locale(), scope.options( numberFmtFactory.name() ) )
                .select( singleParam, exp.variantKeys(), exp.defaultVariantKey(), scope );
    }

    ///  implicit temporal format
    private String formatTemporal(final FluentTemporal fluentTemporal, final Scope scope) {
        return scope.cache().getFunction( temporalFmtFactory, scope.locale(), scope.options( temporalFmtFactory.name() ) )
                .format( fluentTemporal, scope )
                .value();
    }


    /// implicit custom format
    private <T> String formatCustom(final FluentCustom<T> custom, final Scope scope) {
        final var formatFactory = this.lookup( custom.type() );
        if (formatFactory != null) {
            return scope.cache().getFunction( formatFactory, scope.locale(), scope.options( formatFactory.name() ) )
                    .format( custom, scope )
                    .value();
        }

        // no custom formatter found.
        return String.valueOf( custom.value() );
    }

    ///  implicit temporal select
    /// This usually will not match but could if function-specific options have been applied.
    private VariantKey selectTemporal(final FluentTemporal fluentTemporal, final SelectExpression exp, final Scope scope) {
        final ResolvedParameters singleParam = ResolvedParameters.from( fluentTemporal, scope );
        return scope.cache().getFunction( temporalFmtFactory, scope.locale(), scope.options( temporalFmtFactory.name() ) )
                .select( singleParam, exp.variantKeys(), exp.defaultVariantKey(), scope );
    }

    private <T> VariantKey selectCustom(FluentCustom<T> custom, SelectExpression exp, Scope scope) {
        final var formatFactory = this.lookup( custom.type() );
        if (formatFactory != null) {
            final ResolvedParameters singleParam = ResolvedParameters.from( custom, scope );
            return scope.cache().getFunction( formatFactory, scope.locale(), scope.options( formatFactory.name() ) )
                    .select( singleParam, exp.variantKeys(), exp.defaultVariantKey(), scope );
        }

        // no custom formatter found.
        return exp.matchOrDefault( String.valueOf( custom.value() ) ).key();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> FluentFunctionFactory<FluentFunction.Formatter<T>> lookup(Class<T> cls) {
        // exact
        final C2FEntry<T> found = (C2FEntry<T>) customExact.get( cls );
        if (found != null) {
            return found.formatFactory;
        }

        // subtype
        for (final C2FEntry<?> entry : customList) {
            if (cls.isAssignableFrom( entry.cls() )) {
                return ((C2FEntry<T>) entry).formatFactory;
            }
        }

        // no match
        return null;
    }

    // class-to-format factory record for type safety
    private record C2FEntry<T>(Class<T> cls, FluentFunctionFactory<FluentFunction.Formatter<T>> formatFactory) {}

    ///  Build a FluentValueFormatter
    @NullMarked
    public static class Builder {
        // we have to make sure these are typesafe
        private final Map<Class<?>, C2FEntry<?>> exact = new HashMap<>();
        private final SequencedMap<Class<?>, C2FEntry<?>> subtypes = new LinkedHashMap<>();    // insertion order must be preserved
        private final Map<String, FluentFunctionFactory<?>> factories = new HashMap<>();

        // metafactory count
        private int metaCount = 0;

        // required implicit formatters
        private FluentFunctionFactory<FluentFunction.TerminalReducer> reducerFactory;
        private FluentFunctionFactory<FluentFunction.Formatter<TemporalAccessor>> temporalFactory;
        private FluentFunctionFactory<FluentFunction.Formatter<Number>> numberFactory;

        private Builder() {
            // set the default required formatters.
            this.numberFactory = NumberFn.NUMBER;
            factories.put( numberFactory.name(), numberFactory );

            this.temporalFactory = DateTimeFn.DATETIME;
            factories.put( temporalFactory.name(), temporalFactory );

            this.reducerFactory = ListFn.LIST;
            factories.put( reducerFactory.name(), reducerFactory );
        }

        private static boolean matches(final String name, final FluentFunctionFactory<?> factory) {
            return factory.name().equals( name );
        }

        private static void checkRegistered(final Map<Class<?>, ?> mapToCheck, final Class<?> classToCheck, final MetaFunctionFactory<?> metaFactory) {
            if (mapToCheck.containsKey( classToCheck )) {
                throw new IllegalArgumentException(
                        String.format( "Type '%s' (factory: '%s') already registered with factory: '%s')",
                                classToCheck.getName(),
                                metaFactory.name(),
                                mapToCheck.get( classToCheck )
                        )
                );
            }
        }


        public FluentFunctionRegistry build() {
            return new FluentFunctionRegistry( this );
        }


        public Builder setTerminalReducer(final FluentFunctionFactory<FluentFunction.TerminalReducer> factory) {
            this.reducerFactory = requireNonNull( factory );
            factories.put( factory.name(), factory ); // replace any existing, if present
            return this;
        }

        public Builder setTemporalFormatter(final FluentFunctionFactory<FluentFunction.Formatter<TemporalAccessor>> factory) {
            this.temporalFactory = requireNonNull( factory );
            factories.put( factory.name(), factory ); // replace any existing, if present
            return this;
        }

        public Builder setNumberFormatter(final FluentFunctionFactory<FluentFunction.Formatter<Number>> factory) {
            requireNonNull( factory );
            if (!(factory instanceof FluentFunction.Selector)) {
                throw new IllegalArgumentException( String.format( "Factory '%s': Number formatter must have Selector implemented", factory.name() ) );
            }
            this.numberFactory = requireNonNull( factory );
            factories.put( factory.name(), factory ); // replace any existing, if present
            return this;
        }

        // these can be formatters or not, but default formatting will not occur (e.g., for custom types) unless specifically set.
        public Builder addFactory(final FluentFunctionFactory<?> factory) {
            requireNonNull( factory );
            if (factories.putIfAbsent( factory.name(), factory ) != null) {
                throw new IllegalArgumentException( String.format( "Duplicate factory name (already added): '%s'", factory.name() ) );
            }
            return this;
        }


        // CANNOT remove temporal/terminal/numberformatter
        public Builder removeFactory(final String factoryName) {
            requireNonNull( factoryName );
            if (factories.containsKey( factoryName )) {
                if (matches( factoryName, reducerFactory ) || matches( factoryName, temporalFactory ) || matches( factoryName, numberFactory )) {
                    throw new IllegalArgumentException( String.format(
                            """
                                    Cannot remove factory '%s'; it is a required implicit factory.
                                    Use 'setTerminalReducer',  or 'setTemporalFormatter', or 'setNumberFormatter' to specify
                                    a required implicit factory.
                                    """,
                            factoryName ) );
                }
                factories.remove( factoryName );
                return this;
            } else {
                throw new IllegalArgumentException( String.format( "Cannot remove factory '%s'; it has not yet been added / does not exist.", factoryName ) );
            }
        }


        // if factory is a transform, also will be added.
        // cannot add if name clashes
        public <T> Builder addDefaultFormatterExact(final Class<T> cls, final FluentFunctionFactory<FluentFunction.Formatter<T>> factory) {
            requireNonNull( cls );
            requireNonNull( factory );
            if (factories.containsKey( factory.name() )) {
                throw new IllegalArgumentException( String.format( "Duplicate factory name (already added): '%s'", factory.name() ) );
            }

            factories.put( factory.name(), factory );
            exact.put( cls, new C2FEntry<>( cls, factory ) );

            return this;
        }

        // if factory is a transform, also will be added.
        // cannot add if name clashes
        // however ORDER is dependent. e.g., since it applies to subtypes, a base type must be added before a more specialized derived type
        // since we just do a linear search
        public <T> Builder addDefaultFormatter(final Class<T> cls, final FluentFunctionFactory<FluentFunction.Formatter<T>> factory) {
            requireNonNull( cls );
            requireNonNull( factory );

            if (factories.containsKey( factory.name() )) {
                throw new IllegalArgumentException( String.format( "Duplicate factory name (already added): '%s'", factory.name() ) );
            }

            factories.put( factory.name(), factory );
            subtypes.putLast( cls, new C2FEntry<>( cls, factory ) );

            return this;
        }

        // simple formatter via lambda (implicit formatter)
        public <T> Builder addDefaultFormatterExact(final Class<T> cls, final BiFunction<T, Scope, String> fn) {
            // we ONLY add to 'exact'. cannot be called by name, so not in 'factories' name->factory map
            // however, there must not already be a type for this class registered.
            final MetaFunctionFactory<T> metaFactory = ofMeta( cls, fn );
            checkRegistered( exact, cls, metaFactory );
            exact.put( cls, new C2FEntry<>( cls, metaFactory ) );
            return this;
        }

        // simple formatter via lambda (implicit formatter)
        // order-dependent
        public <T> Builder addDefaultFormatter(final Class<T> cls, final BiFunction<T, Scope, String> fn) {
            final MetaFunctionFactory<T> metaFactory = ofMeta( cls, fn );
            checkRegistered( subtypes, cls, metaFactory );
            subtypes.putLast( cls, new C2FEntry<>( cls, metaFactory ) );
            return this;
        }

        private <T> MetaFunctionFactory<T> ofMeta(final Class<T> cls, final BiFunction<T, Scope, String> fn) {
            return new MetaFunctionFactory<>( metaCount++, cls, fn );
        }

    }// class Builder


    /// This is used to convert simple format() lambdas into a factory, for custom FluentValues
    private record MetaFunctionFactory<T>(int count,
                                          Class<T> cls,
                                          BiFunction<T, Scope, String> fn)
            implements FluentFunctionFactory<FluentFunction.Formatter<T>>,
            FluentFunction.Formatter<T>, FluentFunction.Selector {

        @Override
        public Formatter<T> create(final Locale locale, final Options options) {
            return this;
        }

        @Override
        public boolean canCache() {
            return false;
        }

        @Override
        public String name() {
            return "[Meta_" + count + ']';
        }

        @Override
        public FluentValue<String> format(FluentValue<? extends T> in, Scope scope) {
            return FluentString.of( fn.apply( in.value(), scope ) );
        }

        @Override
        public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
            // this should NEVER be called (would only apply if the function was called explicitly)
            throw new IllegalStateException( String.format( "MetaFunction '%s' invoked by name!", name() ) );
        }


        @Override
        public VariantKey select(ResolvedParameters parameters, List<VariantKey> variantKeys, VariantKey defaultKey, Scope scope) {
            final FluentValue<?> fluentValue = Selector.ensureSingle( parameters );
            final Object value = fluentValue.value();
            if (cls.isInstance( value )) {
                final FluentValue<String> formatted = format( FluentCustom.of( cls.cast( value ) ), scope );
                if (formatted instanceof FluentError) {
                    return defaultKey;
                }

                for (final VariantKey variantKey : variantKeys) {
                    if (variantKey.name().equals( formatted.value() )) {
                        return variantKey;
                    }
                }
            }

            return defaultKey;
        }
    }


}
