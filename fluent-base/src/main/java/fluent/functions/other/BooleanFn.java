package fluent.functions.other;

import fluent.bundle.resolver.Scope;
import fluent.functions.FluentFunction;
import fluent.functions.FluentFunctionException;
import fluent.functions.ResolvedParameters;
import fluent.syntax.AST.SelectExpression;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/// custom function that operates on `FluentCustom<Boolean>`
/// useful for select. uses default formatter (which will result in 'true' or 'false')
/// however if used as a function BOOLEAN($arg) can specify conversion to decimal,
/// e.g., BOOLEAN($arg as:number) will convert to 0 or 1.  as:string == 'true' or 'false' (default)
///
@NullMarked
public enum BooleanFn implements FluentFunction {

    BOOLEAN;


    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        FluentFunction.ensureInput( parameters );

        final DisplayAs displayAs = parameters.options()
                .asEnum( DisplayAs.class, "as" )
                .orElse( DisplayAs.STRING );

        final var biConsumer = FluentFunction.mapOrPassthrough( Boolean.class,
                bool -> switch (displayAs) {
                    case STRING -> FluentString.of( String.valueOf( bool ) );
                    case NUMBER -> new FluentNumber.FluentLong( bool ? 1L : 0L );
                }
        );
        return parameters.positionals().mapMulti( biConsumer ).toList();
    }

    @Override
    public FluentValue<?> select(final SelectExpression selectExpression, final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        // TODO: ! implement
        throw new UnsupportedOperationException( "boolean select needs to be implemented" );
    }

    private enum DisplayAs {
        NUMBER, STRING
    }


}
