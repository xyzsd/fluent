package fluent.functions;

import fluent.bundle.resolver.Scope;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.stream.Stream;

///
/// Reduce a value (or multiple values) to a single value.
/// This is the LIST (previously JOIN) formatter, which is a terminal operation.
///
/// Only one `TerminalReducer` is allowed in a FluentBundle.
///
/// This method is ALWAYS called unless there is an error.
///
@NullMarked
// TODO: rename to implicitreducer
public interface TerminalReducer extends FluentFunction {

    // this interface covers the 'implicit' form (e.g., {$listvar})
    // and the FluentFunction interface apply() covers the explicit form { LIST($listvar) }

    ///
    /// Format the FluentObject (of which there may be multiple) to a
    /// single String value.
    ///
    /// This is a terminal operation.
    ///
    /// @param in    value(s) to format
    /// @param scope Scope
    /// @return formatted value(s) as a String
    String reduce(List<FluentValue<?>> in, Scope scope) throws FluentFunctionException;

}
