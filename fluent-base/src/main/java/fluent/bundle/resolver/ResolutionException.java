/*
 *
 *  Copyright (c) 2025, xyzsd (Zach Del)
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
 *
 */

package fluent.bundle.resolver;

import fluent.syntax.AST.Identifiable;
import fluent.syntax.AST.InlineExpression;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/// Exceptions that can occur during Resolution
///
/// Since this class is not intended to be extended by users, it is sealed and also uses
/// checked Exceptions so that we can more easily evaluate exception flow and handling during
/// error conditions.
@NullMarked
public sealed class ResolutionException extends Exception {


    protected ResolutionException(String message) {
        super( requireNonNull( message ) );
    }

    protected ResolutionException(String message, @Nullable Throwable cause) {
        super( requireNonNull( message ), cause );
    }

    ///  getMessage(), but should never be null.
    public String getMessage() {
        return super.getMessage();
    }


    /// Cyclical reference encountered
    public static final class CyclicException extends ResolutionException {

        public CyclicException(Identifiable identifiable) {
            super( "Cyclic dependency: " + requireNonNull( identifiable ).name() );
        }

    }

    ///  Placeable limit exceeded (to prevent expansion-based attacks)
    public static final class TooManyPlaceables extends ResolutionException {
        public TooManyPlaceables() {
            super( "Too many placeables." );
        }
    }

    ///  Reference exceptions
    public static final class ReferenceException extends ResolutionException {

        // if we really need to distinguish categories of ReferenceExceptions, we can either
        // create subclasses or have an enum with type.

        private ReferenceException(String message) {
            super( message );
        }


        ///  Used when there is no Pattern for a Message
        public static ReferenceException noValue(final InlineExpression.MessageReference mr) {
            requireNonNull( mr );
            return noValue( mr.name() );
        }

        ///  Used when there is no Pattern for a Message
        public static ReferenceException noValue(final String messageID) {
            requireNonNull( messageID );
            return new ReferenceException( "No pattern specified for message: '" + messageID +"'");
        }

        ///  Unknown Message or Attribute for a Message
        public static ReferenceException unknownMessageOrAttribute(final InlineExpression.MessageReference mr) {
            requireNonNull( mr );
            return unknownMessageOrAttribute(
                    mr.name(),
                    ((mr.attributeID() == null) ? null : mr.attributeID().name())
            );
        }

        ///  Unknown Message or Attribute for a Message
        public static ReferenceException unknownMessageOrAttribute(final String messageID, final @Nullable String attributeID) {
            requireNonNull( messageID );
            if (attributeID == null) {
                return new ReferenceException( "Unknown message: '" + messageID +"'");
            } else {
                return new ReferenceException( "Unknown attribute: '" + messageID + '.' + attributeID +"'");
            }
        }

        ///  Unknown Term or Attribute for a Term
        public static ReferenceException unknownTermOrAttribute(final InlineExpression.TermReference tr) {
            requireNonNull( tr );
            if (tr.attributeID() == null) {
                return new ReferenceException( "Unknown term: -" + tr.name() );
            } else {
                return new ReferenceException( "Unknown attribute: -" + tr.attributeID().name() + '.' + tr.attributeID().name() );
            }
        }

        ///  Unknown Function
        public static ReferenceException unknownFn(final InlineExpression.FunctionReference fr) {
            requireNonNull( fr );
            return new ReferenceException( "Unknown function: " + fr.name() + "()" );
        }

        ///  Unknown Variable
        public static ReferenceException unknownVariable(final InlineExpression.VariableReference vr) {
            requireNonNull( vr );
            return new ReferenceException( "Unknown variable: $" + vr.name() );
        }


    }

}
