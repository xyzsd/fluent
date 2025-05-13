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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class Message implements Entry, Identifiable {
    private final Identifier identifier;
    private final @Nullable Pattern pattern;
    private final List<Attribute> attributes;   // may be empty
    private final  Commentary.@Nullable Comment comment;

    public Message(Identifier identifier, @Nullable Pattern pattern, List<Attribute> attributes,  Commentary.@Nullable Comment comment) {
        this.identifier = identifier;
        this.pattern = pattern;
        this.attributes = List.copyOf( attributes );
        this.comment = comment;
    }

    /** Find matching attribute, if any */
    public Optional<Attribute> attribute(final String id) {
        return Attribute.match( attributes(), id );
    }

    /** Find matching attribute, if any */
    public Optional<Attribute> attribute(@Nullable final Identifier id) {
        return Attribute.match( attributes(), id );
    }

    /** Create a new Message, with the given Comment. If null, the existing comment is removed (if present). */
    public Message withComment(Commentary.@Nullable Comment newComment) {
        return new Message( identifier, pattern, attributes, newComment );
    }

    public Identifier identifier() {
        return identifier;
    }

    public Optional<Pattern> pattern() {
        return Optional.ofNullable( pattern );
    }

    public List<Attribute> attributes() {
        return attributes;
    }

    public Optional<Commentary.Comment> comment() {
        return Optional.ofNullable( comment );
    }


    @Override
    public String toString() {
        return "Message{" +
                "id=" + identifier +
                ", value=" + pattern +
                ", attributes=" + attributes +
                ", comment=" + comment +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return identifier.equals( message.identifier ) && Objects.equals( pattern, message.pattern ) && attributes.equals( message.attributes ) && Objects.equals( comment, message.comment );
    }

    @Override
    public int hashCode() {
        return Objects.hash( identifier, pattern, attributes, comment );
    }
}
