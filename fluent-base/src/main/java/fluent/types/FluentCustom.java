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
import org.jetbrains.annotations.NotNull;

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
public /*non-sealed*/ class FluentCustom<T> implements FluentValue<T> {

    @NotNull protected final T value;

    public FluentCustom(@NotNull T value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public String format(Scope scope) {
        return String.valueOf(value());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FluentCustom<?> that = (FluentCustom<?>) o;
        return value.equals( that.value );
    }

    @Override
    public int hashCode() {
        return Objects.hash( value );
    }

    @Override
    public String toString() {
        return "FluentCustom{" +
                "value=" + value +
                '}';
    }
}
