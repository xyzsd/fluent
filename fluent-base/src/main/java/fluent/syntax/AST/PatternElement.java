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

import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullMarked;

/**
 * Parent of Placeable and TextElement
 */
@NullMarked
public /*sealed*/ interface PatternElement extends SyntaxNode {

    final record TextElement(String value) implements PatternElement {
    }

    final record Placeable(Expression expression) implements PatternElement, InlineExpression {
        @Override
        public List<FluentValue<?>> resolve(Scope scope) {
            return expression.resolve( scope );
        }
    }
}
