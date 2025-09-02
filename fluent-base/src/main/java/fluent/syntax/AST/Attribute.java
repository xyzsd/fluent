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
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

///  Attribute
@NullMarked
public record Attribute(Identifier identifier, Pattern pattern)
        implements SyntaxNode, Identifiable {

    /// Find matching attribute, if any
    public static Optional<Attribute> match(final List<Attribute> attributeList,
                                            @Nullable final String id) {
        return attributeList.stream()
                .filter( attr -> attr.name().equals( id ) )
                .findAny();
    }

    /// Find matching attribute, if any
    public static Optional<Attribute> match(final List<Attribute> attributeList,
                                            @Nullable final Identifier id) {
        return attributeList.stream()
                .filter( attr -> attr.identifier().equals( id ) )
                .findAny();
    }
}
