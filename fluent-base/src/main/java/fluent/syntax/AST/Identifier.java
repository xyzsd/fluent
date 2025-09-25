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

///  Identifier
///
///  Identifiers may have specific constraints; for example, Function identifiers
///  have a more strict definition than, say, Term or Message identifiers.
@NullMarked
public record Identifier(String name) implements SyntaxNode, VariantKey {

    // FUTURE: This could be a sealed interface, with specific records for
    // specific types of identifiers (e.g., FunctionIdentifier, MessageIdentifier, etc.)

    @Override
    public String name() {
        return name;
    }
}
