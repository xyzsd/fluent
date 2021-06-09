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
 * An error value.
 *
 */
public record FluentError(@NotNull String value) implements FluentValue<String> {

    public FluentError {
        Objects.requireNonNull( value );
    }

    public static FluentError of(@NotNull String s) {
        return new FluentError( s );
    }

    @Override
    public String format(Scope scope) {
        return value;
    }

}
