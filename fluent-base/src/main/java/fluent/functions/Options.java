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

import fluent.syntax.AST.CallArguments;
import fluent.syntax.AST.Literal;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/// An immutable name-value mapping, with a restricted set of allowed mapped values.
///
/// Values may be String, Long, or Double types only. Values are **not** FluentValues; values
/// are not formatted, resolved, or changed.
///
/// Option names are matched exactly and are case sensitive (matched using Locale.ROOT).
///
/// This class has convenience methods which permit Long types to be queried as Integers.
/// Additionally, String types may be queried as enum class constants or boolean values.
///
/// For example,
/// `FUNCTION(optionName:"theValue")`
///
///       - `has("optionName") = true`
///       - `has("optionname") = false` // note the lowercase 'n'
///       - `asBoolean("option-3") = Optional.empty()`
///       - `asString("optionName") = "theValue"`
///       - `asBoolean("optionName") = Exception` // "theValue" is neither "true" nor "false"
///
///
/// For example, `ANOTHERFUNCTION(optionName:"TruE")`, and
/// also given the class `enum MyEnum{TRUE, FALSE, MAYBE}`
///
///       - `has("optionName") = true`
///       - `asString("optionName") = "TruE"`
///       - `asBoolean("optionName") = Boolean.TRUE` // case insensitive match
///       - `asEnum(MyEnum.class, "optionName") = MyEnum.TRUE` // case insensitive enum name match
///
@NullMarked
public class Options {

    ///  No options set
    public static final Options EMPTY = new Options( Map.of() );


    private final Map<String, ?> opts;


    /// private constructor
    private Options(Map<String, ?> in) {this.opts = Map.copyOf( in );}

    /// Create a Builder
    ///
    /// @return Builder
    public static Builder builder() {
        return new Builder();
    }


    ///  If this Options is empty, use `other` (which may also be empty)
    public Options orElse(Options other) {
        return opts.isEmpty() ? other : this;
    }


    /// Convert NamedArguments within CallArguments into Options.
    ///
    /// Nullsafe.
    public static Options from(@Nullable final CallArguments callArgs) {
        if (callArgs == null || callArgs.named().isEmpty()) {
            return EMPTY;
        }

        final Builder builder = new Builder();
        callArgs.named().forEach( n -> builder.set( n.name().key(), n.value() ) );
        return builder.build();
    }

    private static IntStream toIntStream(String optionName, long value) {
        // This will work for all Number types, and is based on
        // JLS primitive narrowing/widening conventions
        final int intValue = (int) value;
        if (intValue != value) {
            throw FluentFunctionException.create(
                    "Option %s: expected value within Integer range (actual: '%d')", optionName, value );
        } else {
            return IntStream.of( (int) value );
        }
    }

    // because Boolean.parseBoolean() is not strict w.r.t. accepted values
    private static Boolean parseBoolStrict(String optionName, String value) {
        if ("true".equalsIgnoreCase( value )) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase( value )) {
            return Boolean.FALSE;
        }
        throw typeError( optionName, "Boolean", value );
    }

    // case-insensitive enum match
    private static <E extends Enum<E>> E matchEnum(final Class<E> enm, final String optionName, final String optionValue) {
        final E[] values = enm.getEnumConstants();
        for (E value : values) {
            if (value.name().equalsIgnoreCase( optionValue )) {
                return value;
            }
        }

        // helpful error message
        throw FluentFunctionException.create( "Option %s: unrecognized value '%s'. Allowed string values: %s",
                optionName,
                optionValue,
                Arrays.toString( enm.getEnumConstants() )
        );
    }

    private static FluentFunctionException typeError(String optionName, String expectedType, Object actual) {
        return FluentFunctionException.create( "Option %s: expected type %s (actual: '%s')",
                optionName,
                expectedType,
                String.valueOf( actual )
        );
    }

    /// True if no options have been set (count() == 0)
    public boolean isEmpty() {
        return opts.isEmpty();
    }

    /// Number of named options.
    public int count() {
        return opts.size();
    }

    /// True if the option name has been set.
    ///
    /// @param name option name to check; exact match required
    public boolean has(String name) {
        return opts.containsKey( name );
    }

    /// Merges the current options with 'toMerge', returning a new Options.
    ///
    /// Any option in toMerge with the same name as in the current set will override (replace)
    /// the value within the new Options returned.
    ///
    /// Implementation note: this is optimized for cases where one--or both--of the Options may be empty.
    @Contract(pure = true)
    public Options mergeOverriding(final Options toMergeAndOverride) {
        // fast-path
        if (toMergeAndOverride == EMPTY) {
            // nothing to merge
            return this;
        } else if (this == EMPTY) {
            // nothing exists, 'everything' to merge
            return toMergeAndOverride;
        } else {
            Map<String, Object> temp = new HashMap<>( this.opts );
            temp.putAll( toMergeAndOverride.opts );
            return new Options( temp );
        }
    }

    /// Get the value of an option, as a String.
    ///
    /// If the value is absent, return an empty Optional.
    ///
    ///
    /// If the value is present, but not a String, throw an exception.
    /// (for example: FUNCTION(name: 37) option "name" is a number; whereas FUNCTION(name: "37")
    /// option "name" would be a String)
    ///
    /// Otherwise, return the value.
    ///
    /// @param optionName option name (case sensitive)
    /// @return option value (as above)
    public Optional<String> asString(final String optionName) {
        return asType( optionName, String.class, null );
    }

    /// Get the value of an option, as a Boolean.
    ///
    /// If the value is absent, return an empty Optional.
    ///
    ///
    /// If the value is "true" or "false" (case-insensitive), return the appropriate Boolean value.
    /// Otherwise, throw an exception.
    ///
    /// @param optionName option name (case sensitive)
    /// @return option value (as above)
    public Optional<Boolean> asBoolean(final String optionName) {
        return asType( optionName, String.class, "Boolean" )
                .map( s -> parseBoolStrict( optionName, s ) );
    }

    /// Get the value of an option, if it matches an existing Enum type
    ///
    /// If the value is absent, return an empty Optional.
    ///
    ///
    /// If the value matches an enum constant, return the enum. Otherwise, throw an exception.
    ///
    ///
    /// NOTE: a case-insensitive match is used to evaluate enums, since the convention for
    /// option format is lower case or camel case, but enum constants are typically upper case.
    ///
    /// @param optionName option name (case sensitive)
    /// @return option value (as above)
    public <E extends Enum<E>> Optional<E> asEnum(final Class<E> enumClass, final String optionName) {
        return asType( optionName, String.class, "Enum (as a String)" )
                .map( value -> matchEnum( enumClass, optionName, value ) );
    }

    /// Get the value of an option, as an int.
    ///
    /// If the value is absent, return an empty OptionalInt.
    ///
    ///
    /// If the value is present (and within range for an integer) return the value; otherwise, throw an Exception.
    ///
    /// @param optionName option name (case sensitive)
    /// @return option value (as above)
    public OptionalInt asInt(final String optionName) {
        return asType( optionName, Long.class, "Integer" ).stream()
                .flatMapToInt( v -> toIntStream( optionName, v ) )
                .findFirst();
    }

    /// Get the value of an option, as a double.
    ///
    /// If the value is absent, return an empty OptionalDouble.
    ///
    /// @param optionName option name (case sensitive)
    /// @return option value (as above)
    public OptionalDouble asDouble(final String optionName) {
        return asType( optionName, Double.class, null ).stream()
                .flatMapToDouble( DoubleStream::of )
                .findFirst();
    }

    /// Get the value of an option, as a long.
    ///
    /// If the value is absent, return an empty OptionalLong.
    ///
    /// @param optionName option name (case sensitive)
    /// @return option value (as above)
    public OptionalLong asLong(final String optionName) {
        return asType( optionName, Long.class, null ).stream()
                .flatMapToLong( LongStream::of )
                .findFirst();
    }

    /// Returns the given option, if present, as a String, Long, or Double depending upon its form.
    ///
    /// @param optionName option name (case sensitive)
    /// @return the value of the option, or an empty optional
    public Optional<?> asRaw(final String optionName) {
        return Optional.ofNullable( opts.get( optionName ) );
    }

    /// Convert the given String, if present, into the type specified by the conversion
    /// (parsing) function fn. If parsing fails due to an exception, that exception is
    /// wrapped (and thrown) as a FluentFunctionException.
    ///
    /// Only String values are supported. Non-String values will be converted to Strings
    /// via String.valueOf().
    ///
    /// If null is returned, an empty optional will be returned by this method.
    ///
    /// @param optionName option name (case sensitive)
    /// @param fn         Function converting the data to the given type
    /// @throws FluentFunctionException if fn returns null or throws an exception
    public <R> Optional<R> into(final String optionName, final Function<String, R> fn) {
        final var val = opts.get( optionName );
        if (val == null) {
            return Optional.empty();
        }

        // consider: inferring type parameter from lambda and casting..
        // but that is both nontrivial and may be overkill. Vs. having
        // overloads for Function<Long,R> and Function<Double,R>

        try {
            // todo: decide if returning an empty optional or NPE/FFE is better...
            return Optional.of( fn.apply( String.valueOf( val ) ) );
        } catch (Exception e) {
            throw FluentFunctionException.wrap( e );
        }
    }

    private <T> Optional<T> asType(final String optionName, final Class<T> type, @Nullable final String typeNameOverride) {
        final Object val = opts.get( optionName );
        if (val == null) {
            return Optional.empty();
        } else if (type.isInstance( val )) {
            return Optional.of( type.cast( val ) );
        } else {
            final String typeName = (typeNameOverride == null) ? type.getSimpleName() : typeNameOverride;
            throw typeError( optionName, typeName, val );
        }
    }

    @Override
    public String toString() {
        return (this == EMPTY) ? "Options{Options.EMPTY}" : ("Options{" + "opts=" + opts + '}');
    }

    /// Builder for Options
    public static class Builder {

        private final Map<String, Object> map = new HashMap<>();

        private Builder() {}


        /// Set an option value as a String. Null values are not allowed.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new value.
        ///
        /// @param name  option name
        /// @param value option value
        /// @return Builder
        /// @throws NullPointerException if name or value is null
        public Builder set(String name, String value) {
            Objects.requireNonNull( name );
            Objects.requireNonNull( value );
            map.put( name, value );
            return this;
        }

        /// Set an option value as a boolean.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new value.
        ///
        /// @param name  option name
        /// @param value option value
        /// @return Builder
        /// @throws NullPointerException if name is null
        public Builder set(String name, boolean value) {
            return set( name, Boolean.toString( value ) );
        }

        /// Set an option value as a double.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new value.
        ///
        /// @param name  option name
        /// @param value option value
        /// @return Builder
        /// @throws NullPointerException if name is null
        public Builder set(String name, long value) {
            Objects.requireNonNull( name );
            map.put( name, value );
            return this;
        }

        /// Set an option value as a double. The value must be finite.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new value.
        ///
        /// @param name  option name
        /// @param value option value
        /// @return Builder
        /// @throws NullPointerException     if name is null
        /// @throws IllegalArgumentException if value is not finite.
        public Builder set(String name, double value) {
            Objects.requireNonNull( name );
            if (!Double.isFinite( value )) {
                throw new IllegalArgumentException( "non-finite value: " + value );
            }
            map.put( name, value );
            return this;
        }

        /// Add all options, replacing any existing options with the same name.
        ///
        /// @param options Options to merge
        /// @return Builder
        /// @throws NullPointerException if options is null
        public Builder with(Options options) {
            Objects.requireNonNull( options );
            map.putAll( options.opts );
            return this;
        }

        /// Remove the given option. If the option is not present, this has no effect.
        ///
        /// @param name name of option to remove
        /// @return Builder
        public Builder remove(String name) {
            Objects.requireNonNull( name );
            map.remove( name );
            return this;
        }


        // internal use only for now
        private Builder set(String name, Literal<?> literal) {
            // Literals are only String/Long/Double [this is assumed but not checked here]
            map.put( name, literal.value() );
            return this;
        }


        /// Build (create) the Options
        public Options build() {
            if (map.isEmpty()) {
                return Options.EMPTY;
            }

            return new Options( map );
        }

    }
}
