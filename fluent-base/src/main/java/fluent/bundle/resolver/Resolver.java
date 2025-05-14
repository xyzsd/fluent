package fluent.bundle.resolver;

import fluent.functions.FluentFunction;
import fluent.functions.FluentFunctionException;
import fluent.functions.FluentImplicit;
import fluent.functions.ResolvedParameters;
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

import java.util.List;


public class Resolver {

    /** The maximum number of placeables which can be expanded in a single call to 'formatPattern' */
    static final int MAX_PLACEABLES = 100;

    /** Unicode bidi isolation characters */
    private static final char FSI = '\u2068';

    /** Unicode bidi isolation characters */
    private static final char PDI = '\u2069';

    // initial size of StringBuilder (todo: determine a good initial size or perhaps allow tuning)
    private static final int SB_SIZE = 128;


    private Resolver() {}


    ///  Resolve a Pattern
    public static List<FluentValue<?>> resolve(final Pattern pattern, final Scope scope) {
        if (scope.isDirty()) {
            return Resolver.error( "[dirty]" );
        }

        // fast-path (resolve)
        if (pattern.elements().size() == 1) {
            final PatternElement patternElement = pattern.elements().getFirst();
            return switch(patternElement) {
              case TextElement(String v) -> List.of( FluentString.of( v ));
              case Placeable placeable -> scope.maybeTrack( pattern, placeable);
            };
        }

        // TODO: maybe we can compute an approximate size based on the pattern?
        //       perhaps during parsing?
        //       or keep track of total sizes / keep a running statistic of min/max/mean
        //       and size dynamically or for worst case. have a minimum size threshold too.
        StringBuilder sb = new StringBuilder( SB_SIZE );
        for (PatternElement patternElement : pattern.elements()) {
            if (scope.isDirty()) {
                return Resolver.error( "[dirty]" );
            }

            if (patternElement instanceof TextElement(String value)) {
                sb.append( value );
            } else if (patternElement instanceof Placeable placeable) {
                scope.incrementAndCheckPlaceables();

                final boolean needsIsolation = scope.bundle().useIsolation()
                        && pattern.elements().size() > 1
                        && placeable.needsIsolation();

                if (needsIsolation) {
                    sb.append( FSI );
                }

                final List<FluentValue<?>> fluentValues = scope.maybeTrack( pattern, placeable );
                sb.append( scope.reduce( fluentValues ) );

                if (needsIsolation) {
                    sb.append( PDI );
                }
            } else {
                throw new IllegalStateException( patternElement.toString() );
            }
        }

        return List.of( FluentString.of( sb.toString() ) );
    }



    ///  Resolve an Expression
    public static List<FluentValue<?>> resolve(final Expression expression, final Scope scope) {
        return switch(expression) {
            case StringLiteral(String value) -> List.of( FluentString.of( value ));
            case NumberLiteral<?> nl -> List.of( FluentNumber.from( nl.value() ));
            case Placeable(Expression pEx) -> resolve(pEx, scope);  // recurse
            case FunctionReference fr -> resolveFunctionReference( fr, scope );
            case VariableReference vr -> resolveVariableReference( vr, scope );
            case TermReference tr -> resolveTermReference( tr, scope );
            case MessageReference mr -> resolveMessageReference(mr, scope);
            case SelectExpression se -> resolveSelectExpression( se, scope );
        };
    }


    private static List<FluentValue<?>> resolveFunctionReference(final FunctionReference fr, final Scope scope) {
        final String name = fr.name();
        final FluentFunction function = scope.bundle().getFunction( name ).orElse( null );
        if (function == null) {
            scope.addError( ReferenceException.unknownFn( name ) );
            return Resolver.error( name + "()" );
        }

        final ResolvedParameters resolvedParameters = scope.resolveParameters( fr.arguments() );

        try {
            return function.apply( resolvedParameters, scope );
        } catch (final FluentFunctionException ex) {
            FluentFunctionException namedEx = ex.withName( function.name() );
            scope.addError( namedEx );
            throw namedEx;
        }
    }

    private static List<FluentValue<?>> resolveVariableReference(final VariableReference vr, final Scope scope) {
        final String name = vr.name();
        final List<FluentValue<?>> result = scope.lookup( name );

        if (result.isEmpty()) {
            scope.addError( ReferenceException.unknownVariable( name ) );
            return Resolver.error( '$' + name );
        } else {
            return result;
        }
    }

    private static List<FluentValue<?>> resolveTermReference(final TermReference tr, final Scope scope) {
        final String name = tr.name();
        final Term term = scope.bundle().getTerm( name ).orElse( null );
        if (term == null) {
            scope.addError( ReferenceException.unknownTerm( name ) );
            return Resolver.error( name );
        }

        scope.setLocalParams( tr.arguments() );

        final Identifier attributeID = tr.attributeID();
        final List<FluentValue<?>> result;
        if (attributeID == null) {
            result = scope.track( term.value(), tr );
        } else {
            result = term.attribute( attributeID )
                    .map( Attribute::pattern )
                    .map( pattern -> scope.track( pattern, tr ) )
                    .orElseGet( () -> {
                        scope.addError( ReferenceException.unknownAttribute( name, '-' + String.valueOf( tr.attributeID() ) ) );
                        return Resolver.error( name + '.' + attributeID );
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
        final Message message = scope.bundle().getMessage( name ).orElse( null );
        if (message == null) {
            scope.addError( ReferenceException.unknownMessage( name ) );
            return Resolver.error( name );
        }

        // the Message can have an empty pattern, but only if there are attributes
        // NOTE: this is an invariant for Message objects
        /*
        if (attributeID == null) {
            // Pattern-only case (no attributes)
            assert (message.pattern() != null);
            return scope.track( message.pattern(), mr );
        }
        */
        if (attributeID == null) {
            if (message.pattern() == null) {
                scope.addError( ReferenceException.noValue( name ) );
                return Resolver.error( name );
            } else {
                return scope.track( message.pattern(), mr );
            }
        }

        // non-null attribute +/- pattern
        return message.attribute( attributeID )
                    .map( Attribute::pattern )
                    .map( pattern -> scope.track( pattern, mr ) )
                    .orElseGet( () -> {
                        scope.addError( ReferenceException.unknownAttribute( name, String.valueOf( attributeID ) ) );
                        return Resolver.error( name + '.' + attributeID );
                    } );

        /*
        // the message can have an empty pattern, but only if there are attributes
        if (attributeID == null) {
            if (message.pattern() == null) {
                scope.addError( ReferenceException.noValue( name ) );
                return Resolver.error( name );
            } else {
                return scope.track( message.pattern(), mr );
            }
        }

        return message.attribute( attributeID )
                .map( Attribute::pattern )
                .map( pattern -> scope.track( pattern, mr ) )
                .orElseGet( () -> {
                    scope.addError( ReferenceException.unknownAttribute( name, String.valueOf( attributeID ) ) );
                    return Resolver.error( name + '.' + attributeID );
                } );

         */
    }



    private static List<FluentValue<?>> resolveSelectExpression(final SelectExpression se, final Scope scope) {
        final List<FluentValue<?>> resolved = resolve(se.selector(),  scope ); // recurse

        return scope.bundle().implicit( FluentImplicit.Implicit.JOIN )
                .select(
                        se,
                        ResolvedParameters.from( resolved, scope ),
                        scope
                );
    }


    /// Convenience method to create an error message (as a FluentError)
    /// and insert it into the format stream, in an attempt to preserve
    /// the remainder of the message.
    ///
    /// @param text FluentError message
    /// @return Single item list of containing a FluentError
    static List<FluentValue<?>> error(final String text) {
        return List.of( FluentError.of( '{' + text + '}' ) );
    }
}
