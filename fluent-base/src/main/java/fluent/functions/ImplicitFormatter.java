package fluent.functions;

import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;


/// Implicit Formatter
///
/// An Implicit Formatter is the function called when not explicitly specified.
/// For example, in the message `You are {$age} years old`, and assuming `$age` is a numeric type
/// (`FluentNumber`), the NUMBER function is called even though it is not specified. It is
/// equivalent to the following message: `You are {NUMBER($age)} years old`.
///
/// If global options (or options specified for number formatting during construction of a FluentBundle)
/// are specified, they would be applied to NUMBER formatting automatically.
///
/// Functions which implement `ImplicitFormatter` must return a `String`.
///
/// Not all functions are called implicitly or need to implement this interface.
///
/// However, an ImplicitFormatter must be present for the core FluentValue types. An ImplicitFormatter
/// also should be specified for FluentCustom types, otherwise the default formatter will be used
/// (which is {@link String#valueOf(Object)}).
///
// todo: maybe rename to fluentformatter ?
public interface ImplicitFormatter<T> {


// todo: likely will want to look at caching formatters and what not, but need to do performance eval first
//

    String format(FluentValue<? extends T> in, Scope scope);

}
