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

import static java.util.Objects.requireNonNull;

/// A Message
///
/// Messages must have a Pattern, an Attribute, or both
/// a Pattern and Attribute.
///
@NullMarked
public record Message(Identifier identifier, @Nullable Pattern pattern,
                      List<Attribute> attributes, Commentary.@Nullable Comment comment) implements Entry, Identifiable {

    public Message {
        requireNonNull( identifier );
        requireNonNull( attributes );
        attributes = List.copyOf( attributes );

        // INVARIANT: must have a pattern, attributes, or both
        if (pattern == null && attributes.isEmpty()) {
            throw new IllegalStateException( String.format(
                    "Message '%s': Cannot have a null pattern AND empty attributes", identifier.name() ) );
        }
    }


    /// Find matching attribute, if any
    public @Nullable Attribute attribute(final String id) {
        for (final Attribute attr : attributes) {
            if (attr.name().equals( id )) {
                return attr;
            }
        }
        return null;
    }

    /// Find matching attribute, if any
    public Optional<Attribute> attribute(@Nullable final Identifier id) {
        return Attribute.match( attributes(), id );
    }

    /// Create a new Message, with the given Comment.
    public Message withComment(Commentary.@Nullable Comment newComment) {
        return new Message( identifier, pattern, attributes, newComment );
    }

    /// True if this Message contains attribute with given ID
    public boolean hasAttribute(final String attributeID) {
        return attribute( attributeID ) != null;
    }

}
