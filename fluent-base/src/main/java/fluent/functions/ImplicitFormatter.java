package fluent.functions;

import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;


/// Implicit Formatter
///
/// An Implicit Formatter is the function that is called when not explicitly specified.
/// for example, in the message `You are {$age} years old`, and assuming `$age` is a numeric type
/// (`FluentNumber`), the NUMBER function is called even though it is not specified.
///
/// If global options (or options specified for number formatting during construction of a FluentBundle)
/// are specified, they would be applied to NUMBER formatting automatically.
///
/// Functions which implement `ImplicitFormatter` must return a `FluentValue<String>` (so either
/// a `FluentError` or `FluentString`).
///
/// Not all functions are called implicitly or need to implement this interface.
/// However, an ImplicitFormatter generally should be present for any FluentValue that must be converted
/// to a String, including custom types.
///
// todo: maybe rename to fluentformatter
public interface ImplicitFormatter<T> {


// todo: likely will want to look at caching formatters and what not, but need to do performance eval first
//

    String format(FluentValue<? extends T> in, Scope scope);

}
