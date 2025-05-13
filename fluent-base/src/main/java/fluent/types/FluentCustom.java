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

import fluent.bundle.resolver.Scope;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;


/**
 * Custom classes should extend FluentCustom.
 * <p>
 *     FluentCustom is used in lieu of a more appropriate type. Formatting of FluentCustom values
 *     is equivalent to calling the wrapped objects toString() method.
 * </p>
 *
 *
 */
@NullMarked
public record FluentCustom<T>(T value) implements FluentValue<T> {

    public FluentCustom {
        Objects.requireNonNull(value);
    }

    public static <V> FluentCustom<V> of(V value) {
        return new FluentCustom<>( value );
    }


    @Override
    public String format(Scope scope) {
        return String.valueOf(value());
    }

}
