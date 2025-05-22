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

package fluent.types;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.temporal.TemporalAccessor;
import java.util.*;

import static java.util.Objects.requireNonNull;


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
    static final FluentString FLUENT_NULL = new FluentString( "null" );

    ///  the Value
    T value();


    /// Map objects to FluentValues.
    ///
    /// It is not guaranteed that the FluentValue type will be the same as the input type.
    /// This does NOT handle collections; it will wrap them in a FluentCustom.
    ///
    /// See FluentValueFactory for default mappings.
    ///
    /// @param any non-null input
    /// @param <T> type
    /// @return FluentValue
    static <V> FluentValue<?> toFluentValue(final V any) {
        requireNonNull( any );
        checkNested(any);   // could put as case Collection<?> instead..

        return switch(any) {
            case FluentValue<?> v -> v; // prevent re-wrapping
            case String s -> FluentString.of(s);
            case Number n -> FluentNumber.from(n);
            case TemporalAccessor t -> FluentTemporal.of(t);
            default -> FluentCustom.of(any);
        };
    }

    /// Nullsafe mapper.
    ///
    /// All null objects will be of the same type (FluentString, with a String value of "null").
    /// nulls will therefore be handled as FluentStrings. A null input should be considered
    /// an error. Nullsafe mapping, however, allows a higher chance of a message to be constructed
    /// in the event of a program error.
    ///
    /// @param any input
    /// @param <T> any type
    /// @return FluentValue created
    static <V> FluentValue<?> toFluentValueNullsafe(final V any) {
        return toFluentValue( (any == null) ? FLUENT_NULL : any );
    }

    /// Convert the given Set to a List of FluentValues.
    /// Note that depending upon the Set implementation, the argument
    /// order may vary.
    ///
    /// Nested collections are not supported!
    ///
    /// Note that only SequencedCollections are allowed, if a colletion is given.
    ///
    /// @return return the Collection as a List of FluentValues.
    static List<FluentValue<?>> toCollection(@Nullable final Object item) {
        return switch(item) {
            case null -> List.of(FLUENT_NULL);
            case SequencedCollection<?> seq -> convertCollection(seq);
            // These could be passed, but do not make sense. Must check AFTER SequencedCollection
            case Collection<?> _, Map<?,?> _ -> throw new IllegalArgumentException("Only SequencedCollections are supported");
            // default
            default -> List.of(toFluentValue( item ));
        };
    }


    //  Helper method
    private static List<FluentValue<?>> convertCollection(final Collection<?> collection) {
        return collection.stream()
                .map(FluentValue::checkNested)
                .<FluentValue<?>>map( FluentValue::toFluentValueNullsafe )
                .toList();
    }

    // Helper method, to check for a Collection within a Collection (which is not allowed)
    private static <V> @Nullable V checkNested(@Nullable final V in) {
        if(in instanceof Collection<?>) {
            throw new IllegalArgumentException("Invalid arguments. Cannot have a collection nested within another collection. "+in);
        }
        return in;
    }
}
