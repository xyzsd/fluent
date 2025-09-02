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

package fluent.syntax.AST;


import org.jspecify.annotations.NullMarked;

import java.util.List;

///  A SyntaxNode usable as VariantKey (used by Variant)
///
///  A kind of sum-type pseudo-reference to an Identifier or Literal.NumberLiteral.
@NullMarked
public sealed interface VariantKey permits Identifier, Literal.NumberLiteral {

    ///  Match a key by name (case-sensitive) or return default.
    static VariantKey matchKey(final String in, final List<VariantKey> variantKeys, final VariantKey defaultKey) {
        for (final VariantKey variantKey : variantKeys) {
            if (variantKey.name().equals( in )) {
                return variantKey;
            }
        }
        return defaultKey;
    }

    /// The pattern that should be used as a key in a map.
    String name();

}
