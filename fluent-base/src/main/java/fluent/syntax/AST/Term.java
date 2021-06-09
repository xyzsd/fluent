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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


public record Term(@NotNull Identifier identifier,
                   @NotNull Pattern value,
                   @NotNull List<Attribute> attributes,
                   @Nullable Commentary.Comment _comment) implements Entry, Identifiable {


    public Term {
        Objects.requireNonNull( identifier );
        Objects.requireNonNull( value );
        attributes = List.copyOf( attributes );
    }

    public Term(@NotNull Identifier id, @NotNull Pattern value, @NotNull List<Attribute> attributes) {
        this( id, value, attributes, null );
    }

    /**
     * Create a new Term, replacing the existing Comment, if any.
     */
    public Term withComment(@Nullable Commentary.Comment newComment) {
        return new Term( identifier, value, attributes, newComment );
    }

    /** Find attribute matching Identifier */
    public Optional<Attribute> attribute(final Identifier id) {
        return Attribute.match( attributes(), id );
    }

    /** Find attribute matching String */
    public Optional<Attribute> attribute(final String id) {
        return Attribute.match( attributes(), id );
    }

    /** The (optional) Comment */
    public Optional<Commentary.Comment> comment() {
        return Optional.ofNullable( _comment );
    }


}
