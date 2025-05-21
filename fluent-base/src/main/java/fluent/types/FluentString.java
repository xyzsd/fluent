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

import java.util.Objects;

/// Wrapper for a String
///
///  String values may not be null. Null strings may be encoded to "null" by a FluentValueCreator if so desired.
///
@NullMarked
public record FluentString(String value) implements FluentValue<String> {

    public FluentString {
        Objects.requireNonNull(value);
    }

    /// Create a FluentString
    public static FluentString of(String s) {
        return new FluentString( s );
    }



}
