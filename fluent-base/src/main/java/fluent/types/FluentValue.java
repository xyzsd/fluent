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

package fluent.types;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.temporal.TemporalAccessor;
import java.util.*;


/// Interface for all FluentValues.
///
/// Supplied arguments for Fluent messages use FluentValues. FluentValues are created from standard Java objects
/// (String, numeric types, etc.) and used internally when processing Fluent messages.
///
/// FluentValues also control formatting of the values during message creation.
///
/// @param <T> type
@NullMarked
public sealed interface FluentValue<T>
        permits FluentCustom, FluentNumber, FluentString, FluentTemporal, FluentError {

    ///  Value used for null
    FluentString FLUENT_NULL = new FluentString( "null" );

    ///  the Value
    T value();


    /// Map (singular) objects to FluentValues.
    ///
    /// It is not guaranteed that the FluentValue type will be the same as the input type,
    /// however this is generally true. Number types are widened to the pattern that is
    /// most appropriate (long, double, or BigDecimal).
    ///
    /// FluentValues passed in will return out without modification (they will
    /// not be wrapped within another FluentValue).
    ///
    /// If a null or a Collection is provided, an exception will be thrown.
    /// For Collections, use {@link #ofCollection(Object)}.
    ///
    ///
    /// @param any non-null input
    /// @param <V> type
    /// @return FluentValue
    static <V> FluentValue<?> of(final V any) {
        return switch(any) {
            case null -> throw new NullPointerException("null values not allowed");
            case FluentValue<?> v -> v; // prevent re-wrapping
            case CharSequence s -> FluentString.of(s);
            case Number n -> FluentNumber.from(n);
            case TemporalAccessor t -> FluentTemporal.of(t);
            case Collection<?> _, Map<?,?> _-> throw invalid(any);  // illegal!
            default -> FluentCustom.of(any);
        };
    }

    /// Null-safe mapper.
    ///
    /// All null objects will be of the same type (FluentString, with a String pattern of "null").
    /// nulls will therefore be handled as FluentStrings. A null input should be considered
    /// an error. Nullsafe mapping, however, allows a higher chance of a message to be constructed
    /// in the event of a program error.
    ///
    /// @param any input
    /// @param <V> any type
    /// @return FluentValue created
    static <V> FluentValue<?> ofNullable(@Nullable final V any) {
        return of( (any == null) ? FLUENT_NULL : any );
    }

    /// Convert a Collection to a List of FluentValues.
    ///
    /// Single items can be specified, and will result in the creation of a single-item list.
    ///
    /// Collections must implement the {@link SequencedCollection} interface, to maintain a well-defined and
    /// consistent iteration order.
    ///
    /// Collections containing nulls will be handled as with {@link #ofNullable(Object)}.
    ///
    /// @param in input; for single items, this will result in a single-item list.
    /// @return return the Collection as a List of FluentValues.
    static List<FluentValue<?>> ofCollection(@Nullable final Object in) {
        return switch(in) {
            case null -> List.of(FLUENT_NULL);
            case SequencedCollection<?> seq -> convertCollection(seq);
            // These could be passed, but do not make sense. Must check AFTER SequencedCollection
            case Collection<?> _, Map<?,?> _ -> throw new IllegalArgumentException("Only SequencedCollections are supported");
            // default (including singular items)
            default -> List.of( of( in ));
        };
    }


    //  Helper method. map(ofNullable()) also checks for (disallowed) nested collections
    private static List<FluentValue<?>> convertCollection(final Collection<?> collection) {
        return collection.stream()
                .<FluentValue<?>>map( FluentValue::ofNullable )
                .toList();
    }

    private static IllegalArgumentException invalid(final Object in) throws IllegalArgumentException {
        return new IllegalArgumentException(String.format(
                """
                Invalid: '%s' FluentValues cannot contain objects of this type. \
                Values cannot be Collection types (or Map).""",
                in.getClass()));
    }

}
