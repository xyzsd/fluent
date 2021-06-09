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

import fluent.functions.FluentFunction;
import fluent.functions.FluentFunctionException;
import fluent.functions.ResolvedParameters;
import fluent.bundle.resolver.ReferenceException;
import fluent.bundle.resolver.Resolvable;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;



// literal, placeable, messagereference, termreference, variablereference, functionreference
public /*sealed*/ interface InlineExpression extends Expression /*permits InlineExpression.FunctionReference,
        InlineExpression.MessageReference, InlineExpression.TermReference, InlineExpression.VariableReference,
        Literal, PatternElement.Placeable*/ {


    // true except for MessageReference/TermReference/StringLiteral
    default boolean needsIsolation() {
        return true;
    }


    final class MessageReference implements InlineExpression, Identifiable {
        private final @NotNull Identifier msgID;
        private final @Nullable Identifier attrID;

        public MessageReference(@NotNull Identifier msgID, @Nullable Identifier attrID) {
            this.msgID = msgID;
            this.attrID = attrID;
        }

        public Identifier identifier() {
            return msgID;
        }

        public Optional<Identifier> attributeID() {
            return Optional.ofNullable( attrID );
        }

        @Override
        public boolean needsIsolation() {
            return false;
        }

        @Override
        public List<FluentValue<?>> resolve(final Scope scope) {
            // find message
            final Message message = scope.bundle().getMessage( name() ).orElse( null );
            if (message == null) {
                scope.addError( ReferenceException.unknownMessage( name() ) );
                return Resolvable.error( name() );
            }

            // the message can have an empty pattern, but only if there are attributes
            if (attrID == null) {
                return message.pattern()
                        .map( pattern -> scope.track( pattern, this ) )
                        //.map( Function.<FluentValue<?>>identity() )
                        .orElseGet( () -> {
                            scope.addError( ReferenceException.noValue( name() ) );
                            return Resolvable.error( name() );
                        } );
            }

            return message.attribute( attrID )
                    .map( Attribute::pattern )
                    .map( pattern -> scope.track( pattern, this ) )
                    //.map( Function.<FluentValue<?>>identity() )
                    .orElseGet( () -> {
                        scope.addError( ReferenceException.unknownAttribute( name(), String.valueOf( attrID ) ) );
                        return Resolvable.error( name() + '.' + attrID );
                    } );
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageReference that = (MessageReference) o;
            return msgID.equals( that.identifier() ) && Objects.equals( attrID, that.attrID );
        }

        @Override
        public int hashCode() {
            return Objects.hash( msgID, attrID );
        }

        @Override
        public String toString() {
            return "MessageReference{" +
                    "id=" + msgID +
                    ", attribute=" + attrID +
                    '}';
        }
    }



    // note: we use CallArguments BUT there are never any positionals.
    final class TermReference implements InlineExpression, Identifiable {
        private final @NotNull Identifier termID;
        private final @Nullable Identifier attrID;
        private final @Nullable CallArguments arguments;

        public TermReference(@NotNull Identifier termID, @Nullable Identifier attrID, @Nullable CallArguments args) {
            this.termID = termID;
            this.attrID = attrID;
            this.arguments = args;
        }

        public Identifier identifier() {
            return termID;
        }

        public Optional<Identifier> attributeID() {
            return Optional.ofNullable( attrID );
        }

        public Optional<CallArguments> arguments() {
            return Optional.ofNullable( arguments );
        }

        @Override
        public boolean needsIsolation() {
            return false;
        }

        @Override
        public List<FluentValue<?>> resolve(final Scope scope) {
            final Term term = scope.bundle().getTerm( name() ).orElse( null );
            if (term == null) {
                scope.addError( ReferenceException.unknownTerm( name() ) );
                return Resolvable.error( name() );
            }

            scope.setLocalParams( arguments );

            if (attrID == null) {
                List<FluentValue<?>> result = scope.track( term.value(), this );
                scope.clearLocalParams();
                return result;
            } else {
                List<FluentValue<?>> result = term.attribute( attrID )
                        .map( Attribute::pattern )
                        .map( pattern -> scope.track( pattern, this ) )
                        //.map( Function.<FluentValue<?>>identity() )
                        .orElseGet( () -> {
                            scope.addError( ReferenceException.unknownAttribute( name(), '-'+String.valueOf( attrID ) ) );
                            return Resolvable.error( name() + '.' + attrID );
                        } );

                scope.clearLocalParams();
                return result;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TermReference that = (TermReference) o;
            return termID.equals( that.termID ) && Objects.equals( attrID, that.attrID ) && Objects.equals( arguments, that.arguments );
        }

        @Override
        public int hashCode() {
            return Objects.hash( termID, attrID, arguments );
        }

        @Override
        public String toString() {
            return "TermReference{" +
                    "id=" + termID +
                    ", attribute=" + attrID +
                    ", arguments=" + arguments +
                    '}';
        }
    }


    final record VariableReference(Identifier identifier) implements InlineExpression, Identifiable {
        @Override
        public List<FluentValue<?>> resolve(final Scope scope) {
            final List<FluentValue<?>> lookup = scope.lookup( name() );

            if(lookup.isEmpty()) {
                scope.addError( ReferenceException.unknownVariable( name() ) );
                return Resolvable.error( '$'+ name()  );
            } else {
                return lookup;
            }
        }
    }

    final class FunctionReference implements InlineExpression, Identifiable {
        @NotNull private final Identifier identifier;
        @Nullable private final CallArguments arguments;

        public FunctionReference(@NotNull Identifier identifier, @Nullable CallArguments args) {
            this.identifier = identifier;
            this.arguments = args;
        }

        public Identifier identifier() {
            return identifier;
        }

        public Optional<CallArguments> arguments() {
            return Optional.ofNullable( arguments );
        }

        @Override
        public List<FluentValue<?>> resolve(final Scope scope) {
            final FluentFunction function = scope.bundle().getFunction( name() ).orElse( null );
            if (function == null) {
                scope.addError( ReferenceException.unknownFn( name() ) );
                return Resolvable.error( name()+"()" );
            }

            final ResolvedParameters resolvedParameters = scope.resolveParameters( arguments );

            try {
                return function.apply( resolvedParameters, scope );
            } catch(final FluentFunctionException ex) {
                FluentFunctionException namedEx = ex.withName( function.name() );
                scope.addError( namedEx );
                throw namedEx;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FunctionReference that = (FunctionReference) o;
            return identifier.equals( that.identifier ) && Objects.equals( arguments, that.arguments );
        }

        @Override
        public int hashCode() {
            return Objects.hash( identifier, arguments );
        }

        @Override
        public String toString() {
            return "FunctionReference{" +
                    "id=" + identifier +
                    ", arguments=" + arguments +
                    '}';
        }
    }


}
