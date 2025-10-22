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

import fluent.syntax.AST.CallArguments;
import fluent.syntax.AST.NamedArgument;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/// An immutable name-pattern mapping, with a restricted set of allowed mapped values.
///
/// Option Names are String values, and matched exactly (so they are case-sensitive). Matching
/// locale is Locale.ROOT.
///
/// Values may be String, Long, or Double types only. Values are *not* FluentValues; values
/// are not formatted, resolved, or changed.
///
/// This class has convenience methods which permit Long types to be queried as Integers.
/// Convenience methods also allow String types to be queried as enum class constants or boolean values.
///
/// For example, given `FUNCTION(optionName:"theValue")`
/// - `has("optionName") = true`  // optionName is present
/// - `has("optionname") = false` // note the lowercase 'n'; case-sensitive
/// - `asBoolean("option-3") = Optional.empty()` // 'option-3' is not present
/// - `asString("optionName") = "theValue"`
/// - `asBoolean("optionName") = Exception` // "theValue" is neither "true" nor "false"
///
///
/// For example, `ANOTHERFUNCTION(optionName:"TruE")`, and also given the class `enum MyEnum{TRUE, FALSE, MAYBE}`
/// - `has("optionName") = true`
/// - `asString("optionName") = "TruE"`
/// - `asBoolean("optionName") = Boolean.TRUE` // case-insensitive match. This would fail for `optionName:"MAYBE"`
/// - `asEnum(MyEnum.class, "optionName") = MyEnum.TRUE` // case-insensitive enum name match
///
@NullMarked
public final class Options {

    ///  An empty set of Options.
    public static final Options EMPTY = new Options( Map.of() );

    // careful: values must be only String/Double/Long
    private final Map<String, ?> opts;


    /// private constructor
    private Options(Map<String, ?> in) {this.opts = Map.copyOf( in );}

    /// Create a Builder
    ///
    /// @return Builder
    public static Builder builder() {
        return new Builder();
    }

    /// Convert NamedArguments within CallArguments into Options.
    ///
    /// Nullsafe.
    public static Options from(@Nullable final CallArguments callArgs) {
        // fast path
        if (callArgs == null) {
            return EMPTY;
        }

        return from(callArgs.named());
    }

    /// Convert NamedArguments into Options.
    public static Options from(final List<NamedArgument> namedArguments) {
        // fast path
        if (namedArguments.isEmpty()) {
            return EMPTY;
        }

        final Builder builder = new Builder();
        namedArguments.forEach( builder::set );
        return builder.build();
    }

    ///  Convenience: single-item Options with the given parameters.
    public static Options of(final String name, final String value) {
        requireNonNull(name);
        requireNonNull( value );
        return new Builder().set(name, value).build();
    }

    ///  Convenience: single-item Options with the given parameters.
    public static Options of(final String name, final long value) {
        requireNonNull(name);
        return new Builder().set(name, value).build();
    }

    ///  Convenience: single-item Options with the given parameters.
    public static Options of(final String name, final double value) {
        requireNonNull(name);
        return new Builder().set(name, value).build();
    }

    // because Boolean.parseBoolean() is not strict w.r.t. accepted values
    // todo: only accept lower-case true/false ?
    private static Boolean parseBoolStrict(String optionName, String value) {
        if ("true".equalsIgnoreCase( value )) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase( value )) {
            return Boolean.FALSE;
        }
        throw typeError( optionName, "Boolean, which must be either 'true' or 'false'", value );
    }

    // case-insensitive enum match, non-locale specific
    private static <E extends Enum<E>> E matchEnum(final Class<E> enm, final String nameToMatch) {
        final E[] values = enm.getEnumConstants();
        for (E value : values) {
            if (value.name().equalsIgnoreCase( nameToMatch )) {
                return value;
            }
        }

        // helpful error message
        throw FluentFunctionException.of( "Named option '%s': unrecognized. Allowed values: %s",
                nameToMatch,  Arrays.toString( enm.getEnumConstants() ) );
    }

    private static FluentFunctionException typeError(String optionName, String expectedType, Object actual) {
        return FluentFunctionException.of( "Named option '%s': expected type %s (actual: '%s')",
                optionName, expectedType, String.valueOf( actual ) );
    }

    ///  If this Options is empty, use `other` (...which may also be empty)
    public Options orElse(Options other) {
        return opts.isEmpty() ? other : this;
    }

    /// True if no options have been set (count() == 0)
    public boolean isEmpty() {
        return opts.isEmpty();
    }

    /// Number of named options.
    public int size() {
        return opts.size();
    }

    /// True if the option name has been set.
    ///
    /// @param name option name to check; case-sensitive exact match
    public boolean has(String name) {
        return opts.containsKey( name );
    }

    /// Merges the current options with 'toMerge', returning a new Options.
    ///
    /// Any option in toMerge with the same name as in the current set will override (replace)
    /// the pattern within the new Options returned.
    ///
    /// Implementation note: this is optimized for cases where one--or both--of the Options may be empty.
    public Options mergeOverriding(final Options toMerge) {
        if (toMerge == EMPTY) {
            // nothing to merge
            return this;
        } else if (this == EMPTY) {
            // nothing currently exists, 'everything' is merged
            return toMerge;
        } else {
            Map<String, Object> temp = new HashMap<>( this.opts );
            temp.putAll( toMerge.opts );
            return new Options( temp );
        }
    }

    /// Get the pattern of an option, as a String.
    ///
    /// If the pattern is absent, return an empty Optional.
    ///
    ///
    /// If the pattern is present, but not a String, throw an exception.
    /// (for example: FUNCTION(name: 37) option "name" is a number; whereas FUNCTION(name: "37")
    /// option "name" would be a String)
    ///
    /// Otherwise, return the pattern.
    ///
    /// @param optionName option name (case-sensitive)
    /// @return option pattern (as above)
    public Optional<String> asString(final String optionName) {
        return asType( optionName, String.class, null );
    }

    /// Get the pattern of an option, as a Boolean.
    ///
    /// If the pattern is absent, return an empty Optional.
    ///
    /// If the pattern is "true" or "false" (case-insensitive), return the appropriate Boolean pattern.
    /// Otherwise, throw an exception.
    ///
    /// @param optionName option name (case-sensitive)
    /// @return option pattern (as above)
    public Optional<Boolean> asBoolean(final String optionName) {
        return asType( optionName, String.class, "Boolean" ).map( s -> parseBoolStrict( optionName, s ) );
    }

    /// Get the pattern of an option, if it matches an existing Enum type
    ///
    /// If the pattern is absent, return an empty Optional.
    ///
    /// If the pattern matches an enum constant, return the enum. Otherwise, throw an exception.
    ///
    /// NOTE: a case-insensitive match is used to evaluate enums, since the convention for
    /// option format is lower case or camel case, but enum constants are typically upper case.
    ///
    /// @param optionName option name (case-sensitive)
    /// @return option pattern (as above)
    public <E extends Enum<E>> Optional<E> asEnum(final Class<E> enumClass, final String optionName) {
        return asType( optionName, String.class, "Enumerated String" )
                .map( value -> matchEnum( enumClass, value ) );
    }

    /// Get the pattern of an option, as an int.
    ///
    /// If the pattern is absent, return an empty OptionalInt.
    ///
    /// If the pattern is present return the pattern as an Integer. This is performed as per
    /// JLS 5.1.3 narrowing conventions (out-of-range integer values will either be Integer.MAX_VALUE
    /// or Integer.MIN_VALUE).
    ///
    /// @param optionName option name (case-sensitive)
    /// @return option pattern (as above)
    public OptionalInt asInt(final String optionName) {
        return asType( optionName, Long.class, null ).map( l -> OptionalInt.of( l.intValue() ) ).orElse( OptionalInt.empty() );
    }

    /// Get the pattern of an option, as a double.
    ///
    /// If the pattern is absent, return an empty OptionalDouble.
    ///
    /// @param optionName option name (case-sensitive)
    /// @return option pattern (as above)
    public OptionalDouble asDouble(final String optionName) {
        return asType( optionName, Double.class, null ).map( OptionalDouble::of ).orElse( OptionalDouble.empty() );
    }

    /// Get the pattern of an option, as a long.
    ///
    /// If the pattern is absent, return an empty Optional.
    ///
    /// @param optionName option name (case-sensitive)
    /// @return option pattern (as above)
    public OptionalLong asLong(final String optionName) {
        return asType( optionName, Long.class, null ).map( OptionalLong::of ).orElse( OptionalLong.empty() );
    }

    /// Returns the given option, if present, as a String, Long, or Double depending upon its form.
    ///
    /// @param optionName option name (case-sensitive)
    /// @return the pattern of the option, or an empty optional
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
    /// @param optionName option name (case-sensitive)
    /// @param fn         Function converting the data to the given type
    /// @throws FluentFunctionException if fn returns null or throws an exception
    public <R> Optional<R> into(final String optionName, final Function<String, R> fn) {
        final var val = opts.get( optionName );
        if (val == null) {
            return Optional.empty();
        }

        // consider: inferring type parameter from lambda and casting
        // but that is both nontrivial and may be overkill. Vs. having
        // overloads for Function<Long,R> and Function<Double,R>

        try {
            // todo: decide if returning an empty optional or NPE/FFE is better...
            return Optional.of( fn.apply( String.valueOf( val ) ) );
        } catch (Exception e) {
            throw FluentFunctionException.of( e );
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


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Options options = (Options) o;
        return opts.equals( options.opts );
    }

    @Override
    public int hashCode() {
        return opts.hashCode();
    }


    /// A lightweight Builder for Options
    public static final class Builder {

        private final Map<String, Object> map = new HashMap<>();

        private Builder() {}


        /// Set an option pattern as a String. Null values are not allowed.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new pattern.
        ///
        /// @param name  option name
        /// @param value option pattern
        /// @return Builder
        /// @throws NullPointerException if name or pattern is null
        public Builder set(String name, String value) {
            requireNonNull( name );
            requireNonNull( value );
            map.put( name, value );
            return this;
        }

        /// Set an option pattern as a boolean.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new pattern.
        ///
        /// @param name  option name
        /// @param value option pattern
        /// @return Builder
        /// @throws NullPointerException if name is null
        public Builder set(String name, boolean value) {
            return set( name, Boolean.toString( value ) );
        }

        /// Set an option pattern as a double.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new pattern.
        ///
        /// @param name  option name
        /// @param value option pattern
        /// @return Builder
        /// @throws NullPointerException if name is null
        public Builder set(String name, long value) {
            requireNonNull( name );
            map.put( name, value );
            return this;
        }

        /// Set an option pattern as a double. The pattern must be finite.
        ///
        /// If a prior option with this name already exists, it will be
        /// replaced with the new pattern.
        ///
        /// @param name  option name
        /// @param value option pattern
        /// @return Builder
        /// @throws NullPointerException     if name is null
        /// @throws IllegalArgumentException if pattern is not finite.
        public Builder set(String name, double value) {
            requireNonNull( name );
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
            requireNonNull( options );
            map.putAll( options.opts );
            return this;
        }

        /// Remove the given option. If the option is not present, this has no effect.
        ///
        /// @param name name of option to remove
        /// @return Builder
        public Builder remove(String name) {
            requireNonNull( name );
            map.remove( name );
            return this;
        }


        /// Set an option via a named argument.
        public Builder set(NamedArgument namedArgument) {
            // value.value else the wrong objects are put in the map
            map.put( namedArgument.name().name(), namedArgument.value().value() );
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
