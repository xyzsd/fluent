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

import java.util.Objects;


///
///     FluentCustom is used for all custom types. The default Formatting of FluentCustom values
///     is equivalent to calling the wrapped objects toString() method, unless a specific formatter
///     has been defined.
///
///     Note that it is possible to have FluentCustom values that are the same as other FluentValues;
///     e.g., `FluentCustom<String>` or `FluentCustom<Number>` are legal, however, it is *not recommended*
///     to use FluentCustom for any FluentValue type already defined. Custom pattern types that wrap an already-defined
///     FluentValue type will not be automatically created (e.g., FluentValue.of(String) will not create a
///    `FluentCustom<String>`).
///
@NullMarked
public record FluentCustom<T>(T value) implements FluentValue<T> {

    public FluentCustom {
        Objects.requireNonNull(value);
    }


    @SuppressWarnings( "unchecked" )
    public Class<T> type() { return (Class<T>) value.getClass(); }


    ///  Create a FluentCustom
    public static <V> FluentCustom<V> of(V value) {
        return new FluentCustom<>( value );
    }


}
