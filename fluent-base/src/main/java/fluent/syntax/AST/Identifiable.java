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

package fluent.syntax.AST;

/**
 * Any SyntaxNode that has an Identifier should be Identifiable.
 * <p>
 * This permits this interface permits direct access to the name of the identifier as a String, and also
 * ensures uniformity of the method name ({@code identifier()} to access the identifier.
 */
public interface Identifiable {

    /** The name of the Identifier, as a String */
    default String name() {
        return identifier().name();
    }

    /** The Identifier object itself */
    Identifier identifier();
}
