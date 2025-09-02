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
package fluent.bundle.resolver;

import fluent.bundle.resolver.ResolutionException.ReferenceException;
import fluent.function.FluentFunction;
import fluent.function.FluentFunctionException;
import fluent.function.Options;
import fluent.function.ResolvedParameters;
import fluent.syntax.AST.*;
import fluent.syntax.AST.InlineExpression.FunctionReference;
import fluent.syntax.AST.InlineExpression.MessageReference;
import fluent.syntax.AST.InlineExpression.TermReference;
import fluent.syntax.AST.InlineExpression.VariableReference;
import fluent.syntax.AST.Literal.NumberLiteral;
import fluent.syntax.AST.Literal.StringLiteral;
import fluent.syntax.AST.PatternElement.Placeable;
import fluent.syntax.AST.PatternElement.TextElement;
import fluent.types.FluentError;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public final class Resolver {

    /// The maximum number of placeables which can be expanded in a single call to 'formatPattern'
    public static final int MAX_PLACEABLES = 100;

    /// Unicode bidi isolation character
    private static final char FSI = '\u2068';

    /// Unicode bidi isolation character
    private static final char PDI = '\u2069';

    /// initial size of StringBuilder
    private static final int SB_SIZE = 128;


    private Resolver() {}


    ///  Resolve a Pattern
    public static List<FluentValue<?>> resolvePattern(final Pattern pattern, final Scope scope) {
        // fast-path (resolve)
        if (pattern.elements().size() == 1) {
            final PatternElement patternElement = pattern.elements().getFirst();
            return switch (patternElement) {
                case TextElement(String v) -> List.of( FluentString.of( v ) );
                case Placeable placeable -> scope.maybeTrack( pattern, placeable );
            };
        }

        // TODO: investigate SB_SIZE and performance tradeoffs, if applicable
        StringBuilder sb = new StringBuilder( SB_SIZE );
        for (final PatternElement patternElement : pattern.elements()) {
            switch (patternElement) {
                case TextElement(String value) -> sb.append( value );
                case Placeable placeable -> {
                    if (scope.incrementAndCheckPlaceables()) {
                        // abort! too many placeables
                        final var ex = new ResolutionException.TooManyPlaceables();
                        scope.addException( ex );
                        return Resolver.error( ex );
                    }

                    final boolean needsIsolation = scope.bundle().useIsolation()
                            && pattern.elements().size() > 1
                            && placeable.needsIsolation();

                    if (needsIsolation) {
                        sb.append( FSI );
                    }

                    final List<FluentValue<?>> fluentValues = scope.maybeTrack( pattern, placeable );
                    sb.append( scope.registry().reduce( fluentValues, scope ) );

                    if (needsIsolation) {
                        sb.append( PDI );
                    }
                }
            }
        }

        return List.of( FluentString.of( sb.toString() ) );
    }


    ///  Resolve an Expression
    public static List<FluentValue<?>> resolveExpression(final Expression expression, final Scope scope) {
        return switch (expression) {
            case StringLiteral(String value) -> List.of( FluentString.of( value ) );
            case NumberLiteral<?> nl -> List.of( FluentNumber.from( nl.value() ) );
            case Placeable(Expression pEx) -> resolveExpression( pEx, scope );  // recurse
            case FunctionReference fr -> resolveFunctionReference( fr, scope );
            case VariableReference vr -> resolveVariableReference( vr, scope );
            case TermReference tr -> resolveTermReference( tr, scope );
            case MessageReference mr -> resolveMessageReference( mr, scope );
            case SelectExpression se -> resolveSelectExpression( se, scope );
        };
    }


    private static List<FluentValue<?>> resolveFunctionReference(final FunctionReference fr, final Scope scope) {
        final Options localNamedParams = scope.options( fr.name() )
                .mergeOverriding( Options.from( fr.arguments() ) );
        final String functionName = fr.name();

        final FluentFunction.Transform transform;
        try {
            transform = scope.registry().getFunction( functionName, FluentFunction.Transform.class,
                    scope.cache(), scope.locale(), localNamedParams );
        } catch (FluentFunctionException e) {
            // could occur if there is an issue with function creation
            scope.addException( e );
            return Resolver.error( e, functionName );
        }

        // this occurs iff function is not found, or does not implement FluentFunction.Transform
        if (transform == null) {
            final var ex = ReferenceException.unknownFn( fr );
            scope.addException( ex );
            return Resolver.error( ex );
        }

        final ResolvedParameters resolvedParameters = scope.resolveParameters( fr.arguments() );

        try {
            return transform.apply( resolvedParameters, scope );
        } catch (final FluentFunctionException ex) {
            scope.addException( ex.withName( functionName ) );
            return Resolver.error( ex, functionName );
        }
    }

    ///  Resolve a function reference, but as a selector instead of the transform.
    ///  (use select() instead of apply())
    ///
    ///  This is only called if there is a named function on which to select,
    ///  such as NUMBER($var).  Otherwise, resolveSelectExpression should not
    ///  call this method. (e.g., for select on '$var', which is say a FluentNumber)
    ///
    ///  NOTE: the semantics of error handling is different here than in most methods --
    ///  we will throw an exception rather than use Resolver.error(), since we need to return
    ///  a VariantKey.
    ///
    /// @throws FluentFunctionException if an error occurs
    private static VariantKey resolveFunctionSelect(final SelectExpression selectExpression, final FunctionReference fr, final Scope scope) throws ResolutionException {
        final String functionName = fr.name();
        if (!scope.registry().contains( functionName )) {
            throw ReferenceException.unknownFn( fr );
        }

        final Options localNamedParams = scope.options( fr.name() ) // default args.. if any
                .mergeOverriding( Options.from( fr.arguments() ) );
        final FluentFunction function = scope.registry()
                .getFunction( functionName, FluentFunction.class, scope.cache(), scope.locale(), localNamedParams );
        final ResolvedParameters resolvedParameters = scope.resolveParameters( fr.arguments() );


        if (function instanceof FluentFunction.Selector selectorFn) {
            // simple case (#1, above)
            return selectorFn.select( resolvedParameters, selectExpression.variantKeys(), selectExpression.defaultVariantKey(), scope );
        } else {

            if (function instanceof FluentFunction.Transform transformFn) {
                final List<FluentValue<?>> result = transformFn.apply( resolvedParameters, scope );
                return scope.registry().implicitSelect( result, selectExpression, scope );
            } else {
                // unexpected! should not occur, though
                throw FluentFunctionException.of( "%s(): not a FluentFunction.Transform!", functionName );
            }
        }
    }


    private static List<FluentValue<?>> resolveVariableReference(final VariableReference vr, final Scope scope) {
        final String name = vr.name();
        final List<FluentValue<?>> result = scope.lookup( name );

        if (result.isEmpty()) {
            final var ex = ReferenceException.unknownVariable( vr );
            scope.addException( ex );
            return Resolver.error( ex );
        } else {
            return result;
        }
    }

    private static List<FluentValue<?>> resolveTermReference(final TermReference tr, final Scope scope) {
        final String name = tr.name();
        final Term term = scope.bundle().term( name ).orElse( null );
        if (term == null) {
            final var ex = ReferenceException.unknownTermOrAttribute( tr );
            scope.addException( ex );
            return Resolver.error( ex );
        }

        scope.setLocalParams( tr.namedArguments() );

        final Identifier attributeID = tr.attributeID();
        final List<FluentValue<?>> result;
        if (attributeID == null) {
            result = scope.track( term.value(), tr );
        } else {
            result = term.attribute( attributeID )
                    .map( Attribute::pattern )
                    .map( pattern -> scope.track( pattern, tr ) )
                    .orElseGet( () -> {
                        final var ex = ReferenceException.unknownTermOrAttribute( tr );
                        scope.addException( ex );
                        return Resolver.error( ex );
                    } );

        }

        scope.clearLocalParams();

        return result;
    }


    // resolve
    private static List<FluentValue<?>> resolveMessageReference(final MessageReference mr, final Scope scope) {
        final String name = mr.name();
        final Identifier attributeID = mr.attributeID();

        // find referenced Message
        final Message message = scope.bundle().message( name ).orElse( null );
        if (message == null) {
            final var ex = ReferenceException.unknownMessageOrAttribute( mr );
            scope.addException( ex );
            return Resolver.error( ex );
        }

        // the message can have an empty pattern, but only if there are attributes
        if (attributeID == null) {
            // however, the pattern could be absent if the there is no corresponding pattern
            // for the base type (see smoke tests)
            if (message.pattern() == null) {
                final var ex = ReferenceException.noValue( mr );
                scope.addException( ex );
                return Resolver.error( ex );
            } else {
                return scope.track( message.pattern(), mr );
            }
        }

        return message.attribute( attributeID )
                .map( Attribute::pattern )
                .map( pattern -> scope.track( pattern, mr ) )
                .orElseGet( () -> {
                    final var ex = ReferenceException.unknownMessageOrAttribute( mr );
                    scope.addException( ex );
                    return Resolver.error( ex );
                } );
    }


    private static List<FluentValue<?>> resolveSelectExpression(final SelectExpression se, final Scope scope) {
        // New select logic
        // ================
        // (1) explicit function called, AND that function implements FluentFunction.Selector
        //     * apply the selector (easy!)
        // (2) explicit function called, which DOES NOT implement Selector (e.g., COUNT())
        //     * apply function (e.g., for COUNT(), this is like a reduction);
        //          e.g. COUNT("a","b","c") => FluentNumber with value 3
        //     * if this results in a SINGLE value, select on that value using implicit selection logic.
        //       for COUNT(), this would return a FluentNumber, and then we would then use the
        //       implicit appropriate selector (in this case, since it is a FluentNumber, NUMBER() select).
        //       NUMBER() select would then return a plural form (since that is the default select() operation for NUMBER).
        //     * otherwise (e.g., no value or multiple values, or error occurred), error
        // (3) implicit function (based on type), e.g., $arg where '$arg' is a number
        //     * NOTE: this case is not handled here (only explicit functions handled here)
        //     * we call the implicit function/selector. if there is none, we format and do a string match.

        final VariantKey variantKey;
        if (se.selector() instanceof FunctionReference functionReference) {
            try {
                // handle cases #1 and #2, as above
                variantKey = resolveFunctionSelect( se, functionReference, scope );
            } catch (FluentFunctionException e) {
                // here, we can add the function name to the error since it was explicitly called
                scope.addException( e );
                return error( e, functionReference.name() );
            } catch (ResolutionException e) {
                scope.addException( e );
                return error( e );
            }
        } else {
            try {
                // handle case #3, as above
                final List<FluentValue<?>> resolved = resolveExpression( se.selector(), scope ); // recurse
                // implicitSelect() throws for error cases (e.g., 'resolved' not a list containing a single item)
                variantKey = scope.registry().implicitSelect( resolved, se, scope );
            } catch (FluentFunctionException e) {
                // here, the error occurred in an implicit function or elsewhere
                scope.addException( e );
                return error( e, "???" );
            }
        }

        // now, given the variantKey, match it with the Variant and resolve the selected Variant.
        return resolvePattern( se.variantFromKey( variantKey ).pattern(), scope );
    }



    /// Convenience method to create an error message (as a FluentError)
    /// from the given exception, and insert it into the format stream, in an attempt to preserve
    /// the remainder of the message.
    ///
    /// @param e ResolutionException
    /// @return Single item list of containing a FluentError
    static List<FluentValue<?>> error(final ResolutionException e) {
        return List.of( FluentError.of( '{' + e.getMessage() + '}' ) );
    }

    /// Convenience method to create an error message (as a FluentError)
    /// from the given FluentFunctionException
    /// and insert it into the format stream, in an attempt to preserve
    /// the remainder of the message.
    ///
    /// @param e      FluentFunctionException
    /// @param fnName Name of the FluentFunction
    /// @return Single item list of containing a FluentError
    public static List<FluentValue<?>> error(final FluentFunctionException e, final String fnName) {
        return List.of( FluentError.of( '{' + fnName + "(): " + e.getMessage() + '}' ) );
    }
}
