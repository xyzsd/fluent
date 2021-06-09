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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 *  Interface for factories used to create the appropriate FluentValue for a given Object
 *
 */
public interface FluentValueFactory {

    /**
     * Map objects to FluentValues.
     * <p>
     *     It is not guaranteed that the FluentValue type will be the same as the input type.
     * </p>
     * <p>
     *      See FluentValueFactory for default mappings.
     * </p>
     * @param any non-null input
     * @param <T> type
     * @return FluentValue
     */
    <T> FluentValue<?> toFluentValue(@NotNull final T any);

    /**
     * Nullsafe mapper.
     * <p>
     *  All null objects will be of the same type (FluentString, with a String value of "null").
     *  nulls will therefore be handled as FluentStrings. A null input should be considered
     *  an error. Nullsafe mapping, however, allows a higher chance of a message to be constructed
     *  in the event of a program error.
     * </p>
     * @param any input
     * @param <T> any type
     * @return FluentValue created
     */
    default <T> FluentValue<?> toFluentValueNullsafe(@Nullable final T any) {
        return toFluentValue( (any == null) ? FluentString.FLUENT_NULL : any );
    }


    /**
     * Collection mapper.
     * @return return the Collection as a List of FluentValues.
     */
    <T> List<FluentValue<?>> toCollection(@NotNull final T in);

}
