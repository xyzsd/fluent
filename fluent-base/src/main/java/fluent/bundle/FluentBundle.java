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
package fluent.bundle;

import fluent.bundle.resolver.ResolutionException;
import fluent.bundle.resolver.Resolver;
import fluent.bundle.resolver.Scope;
import fluent.function.FluentFunctionException;
import fluent.function.Options;
import fluent.syntax.ast.*;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static fluent.bundle.resolver.ResolutionException.ReferenceException;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;


/// FluentBundle is the primary API for formatting localized messages defined in Fluent (FTL) resources.
///
/// A bundle encapsulates a set of parsed messages/terms, a target Locale, and a registry of formatting
/// functions. Use the Builder to construct a bundle by adding one or more FluentResource instances and
/// then call one of the `format(...)` methods to render a message or one of its attributes.
///
///
/// Typical usage:
/// {@snippet :
///    // Build a bundle
///    FluentFunctionRegistry registry = FluentFunctionRegistry.withDefaults();
///    FluentFunctionCache cache = FluentFunctionCache.defaultCache();
///    FluentResource res = Fluent.parse("hello = Hello, { $name }!\n");
///
///    FluentBundle bundle = FluentBundle
///        .builder(Locale.US, registry, cache)
///        .addResource(res)
///        .build();
///
///    // Format a message
///    String hello = bundle.format("hello", Map.of("name", "World"));
///    // -> "Hello, World!"
///}
///
/// Notes:
/// - Formatting never throws for expected runtime issues. Formatting should only throw if null arguments are passed
///     (not allowed) or if there is a serious, unrecoverable error. For 'normal' expected issues, such as missing
///     message keys, attributes, variables, etc., simple diagnostic error text will be inserted into the rendered
///     message. Generally, as much of the message that can be preserved will be rendered.
/// - Optionally, a logger can be attached to the FluentBundle during construction to obtain more precise error
///     information.
/// - FluentBundle instances are immutable and thread-safe once built; the Builder is not.
///
@NullMarked
public class FluentBundle {

    private final Locale locale;
    private final boolean useIsolation;
    private final Map<String, Term> terms;
    private final Map<String, Message> messages;

    private final FluentFunctionRegistry registry;   // accessed only via Scope
    private final FluentFunctionCache cache;
    private final Map<String, Options> defaultOptions;
    private final @Nullable Consumer<ErrorContext> errorConsumer;


    private FluentBundle(Builder b) {
        this.locale = b.locale;
        this.useIsolation = b.useIsolation;
        this.terms = Map.copyOf( b.terms );
        this.messages = Map.copyOf( b.messages );
        this.registry = b.registry;
        this.cache = b.cache;
        this.defaultOptions = Map.copyOf( b.defaultOptions );
        this.errorConsumer = b.logger;
    }


    ///  for diagnostics
    @Override
    public String toString() {
        return "FluentBundle{" +
                "locale=" + locale +
                ", useIsolation=" + useIsolation +
                ", # of terms= " + terms.size() +
                ", # of messages = " + messages.size() +
                ", cache=" + cache +
                ", registry=" + registry +
                '}';
    }

    /// Create a Builder to build a new FluentBundle.
    ///
    /// Uses the default cache if no cache specified.
    ///
    /// @param locale Locale for the bundle to be built
    /// @return Builder
    public static Builder builder(Locale locale, FluentFunctionRegistry registry, final FluentFunctionCache cache) {
        return new Builder( locale, registry, cache );
    }

    /// Create a Builder to build a new FluentBundle, using an existing FluentBundle as a base.
    @SuppressWarnings( "unused" )
    public static Builder builderFrom(FluentBundle bundle, FluentFunctionCache cache) {
        return new Builder( bundle, cache );
    }

    /// Bundle Locale
    public Locale locale() {
        return locale;
    }

    /// Bundle unicode isolation setting
    public boolean useIsolation() {
        return useIsolation;
    }

    ///  Retrieve the default Options (if any) set for a given function.
    ///  If none were set, this will return `Options.EMPTY`
    public Options options(final String id) {
        requireNonNull( id );
        return defaultOptions.getOrDefault( requireNonNull( id ), Options.EMPTY );
    }

    /// Returns the Message for the given id
    public Optional<Message> message(final String id) {
        requireNonNull( id );
        return Optional.ofNullable( messages.get( id ) );
    }

    /// Returns all Messages
    public Map<String, Message> messages() {
        return messages;
    }

    /// Returns the Term for the given id
    ///
    /// Use [#patternFormat(Pattern, Map, String, String)]
    public Optional<Term> term(final String id) {
        requireNonNull( id );
        return Optional.ofNullable( terms.get( id ) );
    }

    /// Returns all Terms
    public Map<String, Term> terms() {
        return terms;
    }

    ///  Get the cache
    public FluentFunctionCache cache() {
        return cache;
    }

    ///  Get the registry.
    public FluentFunctionRegistry registry() {return registry;}


    ///  Format method for Messages.
    ///
    ///  Gets the message with the given name, and displays it. If there is an error,
    ///  the error message is output instead.
    ///
    ///  This method should not throw in normal usage
    ///
    /// @param messageID Message ID
    /// @param args      Name-Value pairing of arguments
    /// @return the formatted message.
    /// @throws NullPointerException if messageID or args is null
    public String format(final String messageID, final Map<String, Object> args) {
        requireNonNull( messageID );
        requireNonNull( args );

        final PatternOrError poe = getMessagePattern( messageID );

        if (poe.pattern != null) {
            return patternFormat( poe.pattern, args, messageID, null );
        } else {
            assert poe.exceptionSupplier != null;
            try {
                return '{' + poe.exceptionSupplier.get().getMessage() + '}';
            } finally {
                consumeError( messageID, null, poe.exceptionSupplier );
            }
        }
    }

    ///  Get the Pattern for a Message.
    private PatternOrError getMessagePattern(final String messageID) {
        requireNonNull( messageID );

        final Message message = messages.get( messageID );

        if (message == null) {
            return PatternOrError.of( () -> ReferenceException.unknownMessageOrAttribute( messageID, null ) );
        } else if (message.pattern() == null) {
            return PatternOrError.of( () -> ReferenceException.noValue( messageID ) );
        } else {
            return PatternOrError.of( message.pattern() );
        }
    }

    ///  Format method for attributes.
    ///
    ///  Gets the message with the given name, and displays it. If there is an error,
    ///  the error message is output instead.
    ///
    ///  This method should not throw in normal usage
    ///
    /// @param messageID   Message ID
    /// @param attributeID Attribute ID containing the pattern to format.
    /// @param args        Name-Value pairing of arguments
    /// @return the formatted message.
    /// @throws NullPointerException if messageID, attributeID, or args is null
    public String format(final String messageID, final String attributeID, final Map<String, Object> args) {
        requireNonNull( messageID );
        requireNonNull( attributeID );
        requireNonNull( args );

        final PatternOrError poe = getAttributePattern( messageID, attributeID );

        if (poe.pattern != null) {
            return patternFormat( poe.pattern, args, messageID, attributeID );
        } else {
            assert poe.exceptionSupplier != null;
            try {
                return '{' + poe.exceptionSupplier.get().getMessage() + '}';
            } finally {
                consumeError( messageID, null, poe.exceptionSupplier );
            }
        }
    }

    ///  Get the Pattern for an Attribute
    private PatternOrError getAttributePattern(final String messageID, final String attributeID) {
        requireNonNull( messageID );
        requireNonNull( attributeID );

        final Message message = messages.get( messageID );
        if (message != null) {
            final Attribute attribute = message.attribute( attributeID );
            if (attribute != null) {
                return PatternOrError.of( attribute.pattern() );
            }
        }

        // error: either Message or Attribute is missing
        return PatternOrError.of( () -> ReferenceException.unknownMessageOrAttribute( messageID, attributeID ) );
    }

    ///  The simplest format() method.
    ///
    ///  Gets the message with the given name, and renders it. If there is an error,
    ///  the error message (as a String) is output instead.
    ///
    ///  This method should not throw in normal usage.
    ///
    /// @param messageID The name of the message to format.
    /// @return the formatted message.
    /// @throws NullPointerException if messageID is null.
    public String format(String messageID) {
        return format( messageID, Map.of() );
    }


    ///  The simplest format() method for attributes.
    ///
    ///  Gets the message with the given name, and renders it. If there is an error,
    ///  the error message (as a String) is output instead.
    ///
    ///  This method should not throw in normal usage.
    ///
    /// @param messageID   Message ID
    /// @param attributeID Attribute ID containing the pattern to format.
    /// @return the formatted message.
    /// @throws NullPointerException if messageID or attributeID is null.
    public String format(String messageID, String attributeID) {
        return format( messageID, attributeID, Map.of() );
    }


    ///  Error handler for logging/etc.
    private void consumeError(final @Nullable String msgID, final @Nullable String attrID, final Scope scope) {
        if (errorConsumer != null && scope.containsExceptions()) {
            final ErrorContext context = ErrorContext.of( msgID, attrID, locale(), scope.exceptions() );
            errorConsumer.accept( context );
        }
    }

    ///  Error handler for a single exception w/o a Scope
    private void consumeError(final @Nullable String msgID, final @Nullable String attrID, Supplier<Exception> e) {
        if (errorConsumer != null) {
            final ErrorContext context = ErrorContext.of( msgID, attrID, locale(), List.of( e.get() ) );
            errorConsumer.accept( context );
        }
    }


    /// format-builder : WIP
    public FmtBuilder fmtBuilder(final String messageID) {
        return new FmtBuilder( messageID );
    }


    // format the pattern. The msgID and attrID are optional (can be null) and arw only for
    // providing error context information.
    private String patternFormat(final Pattern pattern, final Map<String, ?> args,
                                 final @Nullable String msgID, final @Nullable String attrID) {
        final Scope scope = new Scope( this, args );
        final List<FluentValue<?>> resolved = Resolver.resolvePattern( pattern, scope );
        try {
            return registry.reduce( resolved, scope );
        } catch (FluentFunctionException e) {
            scope.addException( e );    // for error logging
            return '{' + e.getMessage() + '}';
        } finally {
            consumeError( msgID, attrID, scope );
        }
    }


    // TODO: document
    // public version of pattern format
    // e.g., to format a Term/Term attribute
    public String patternFormat(final Pattern pattern, final Map<String, Object> args) {
        return patternFormat( pattern, args, null, null );
    }


    /// Build a FluentBundle
    ///
    /// If required parameters are not set, an exception will be thrown during build.
    ///
    /// If Functions are to be added, replaced, or removed outside a FluentFunctionFactory, the factory must
    /// be set in this Builder prior to adding/replacing/removing functions.
    ///
    /// Builders can be re-used. Builders are not guaranteed to be threadsafe.
    ///
    @NullMarked
    public static final class Builder {
        final Map<String, Options> defaultOptions = new HashMap<>();
        FluentFunctionRegistry registry;
        Locale locale;
        FluentFunctionCache cache;
        Map<String, Term> terms = new HashMap<>();
        Map<String, Message> messages = new HashMap<>();
        boolean useIsolation = false;
        @Nullable Consumer<ErrorContext> logger = null;


        // Builder with Locale, Registry, and Cache
        private Builder(final Locale locale, final FluentFunctionRegistry registry, final FluentFunctionCache cache) {
            this.locale = requireNonNull( locale );
            this.registry = requireNonNull( registry );
            this.cache = requireNonNull( cache );
        }

        // Builder from existing bundle, share registry.
        private Builder(final FluentBundle from, final FluentFunctionCache cache) {
            requireNonNull( from );
            requireNonNull( cache );

            this.locale = from.locale;
            this.useIsolation = from.useIsolation;
            this.terms = new HashMap<>( from.terms );
            this.messages = new HashMap<>( from.messages );
            this.registry = from.registry;
            this.cache = cache;
        }

        /// Use the given Locale when creating this bundle.
        ///
        /// A Locale is required for a bundle to be created
        /// without error.
        ///
        /// @param locale Locale
        /// @return Builder
        @SuppressWarnings( "unused" )
        public Builder withLocale(final Locale locale) {
            this.locale = requireNonNull( locale );
            return this;
        }

        /// Set whether this builder should use Unicode isolating characters
        ///
        /// For more information, see:
        /// [Project Fluent](https://github.com/projectfluent/fluent.js/wiki/Unicode-Isolation)
        /// [unicode.org](https://unicode.org/reports/tr9/#Directional_Formatting_Characters)
        /// [W3C](https://www.w3.org/International/articles/inline-bidi-markup/#nomarkup)
        ///
        /// @return Builder
        @SuppressWarnings( "unused" )
        public Builder withIsolation(final boolean value) {
            useIsolation = value;
            return this;
        }


        /// Add the given resource.
        ///
        /// This can be used multiple times, to add multiple resources.
        ///
        /// If a Term or Message in the resource added collides with a Term or Message already added, a
        /// ReferenceException will be thrown.
        ///
        /// @param resource FluentResource to add
        /// @return Builder
        /// @throws FluentBundleException if term or message entry already exists.
        public Builder addResource(final FluentResource resource) throws FluentBundleException {
            requireNonNull( resource );

            List<String> clashes = new ArrayList<>();

            for (Entry entry : resource.entries()) {
                if (entry instanceof Message msg) {
                    final Message existing = messages.putIfAbsent( msg.name(), msg );
                    if (existing != null) {
                        clashes.add( msg.name() );
                    }
                } else if (entry instanceof Term term) {
                    final Term existing = terms.putIfAbsent( term.name(), term );
                    if (existing != null) {
                        clashes.add( '-' + term.name() );
                    }
                }
            }

            if (!clashes.isEmpty()) {
                throw new FluentBundleException( "Duplicate Messages or Terms: " +
                        String.join( ", ", clashes ) );
            }

            return this;
        }


        /// Add the given resource.
        ///
        /// This can be used multiple times, to add multiple resources.
        ///
        /// Newly-added Terms and Messages will replace existing Terms or Messages with the
        /// same name.
        ///
        /// Associated Comments for duplicated terms or messages are not added or replaced
        /// (this only applies when comments are present, and we are parsing in extended mode).
        ///
        /// @param resource FluentResource to add
        /// @return Builder
        @SuppressWarnings( "unused" )
        public Builder addResourceOverriding(final FluentResource resource) {
            requireNonNull( resource );
            for (Entry entry : resource.entries()) {
                if (entry instanceof Message msg) {
                    messages.put( msg.name(), msg );
                } else if (entry instanceof Term term) {
                    terms.putIfAbsent( term.name(), term );
                }
                // else Commentary, which we ignore
            }
            return this;
        }


        ///  Set the FluentFunctionRegistry
        ///
        /// This replaces the existing FluentFunctionRegistry. A FluentBundle can have only a
        /// single FluentFunctionRegistry.
        @SuppressWarnings( "unused" )
        public Builder withRegistry(final FluentFunctionRegistry registry) {
            this.registry = requireNonNull( registry );
            return this;
        }

        ///  Set the function cache
        ///
        /// This replaces the existing FluentFunctionCache. A FluentBundle can have only a
        /// single FluentFunctionCache, though the cache can be shared (if allowed; depends on cache architecture)
        /// between multiple FluentBundles.
        @SuppressWarnings( "unused" )
        public Builder withCache(final FluentFunctionCache cache) {
            this.cache = requireNonNull( cache );
            return this;
        }


        ///  Programmatically set default options for a given function.
        ///
        /// Options set by this method become the default for a given FluentFunction. However, these default
        /// options can be overridden within the Fluent FTL.
        ///
        /// If the function does not exist, this will throw an exception.
        ///
        /// If this method is called more than once for the same function, subsequent options will replace
        /// the earlier options.
        ///
        /// Options.EMPTY can be used to remove any existing options, if present.
        ///
        /// {@snippet :
        ///        FluentBundle.Builder builder;
        ///        // ...
        ///        // all calls to STRINGSORT() will be reversed, unless "order" is specified otherwise
        ///        builder.withFunctionOptions("STRINGSORT", Options.of("order","reversed"));
        ///        // ...
        ///}
        ///
        /// @param functionName function that exists within the FLuentFunctionRegistry
        /// @param options      function options
        /// @return Builder
        /// @throws IllegalArgumentException if the function name was not found in the FluentFunctionRegistry.
        ///
        public Builder withFunctionOptions(final String functionName, final Options options) {
            requireNonNull( functionName );
            requireNonNull( options );

            if (!registry.contains( functionName )) {
                throw new IllegalArgumentException( String.format(
                        """
                                Cannot find the function factory named '%s';\
                                check spelling and make sure this function was set in the FluentFunctionRegistry.
                                """, functionName ) );
            }

            if (options == Options.EMPTY) {
                defaultOptions.remove( functionName );
            } else {
                defaultOptions.put( functionName, options );
            }

            return this;
        }

        ///  Add a logger, that will log any exceptions that occur during message rendering.
        ///  If this is set to `null` (default), no logging will occur.
        public Builder withLogger(final @Nullable Consumer<ErrorContext> logger) {
            this.logger = logger;
            return this;
        }

        /// Create the FluentBundle.
        ///
        /// @return a new FluentBundle
        public FluentBundle build() {
            requireNonNull( locale, "Locale not set." );
            requireNonNull( registry, "FluentFunctionRegistry not set." );
            return new FluentBundle( this );
        }


    }

    ///  custom tuple, really like a Result/Either
    @NullMarked
    private static class PatternOrError {
        final @Nullable Pattern pattern;
        final @Nullable Supplier<Exception> exceptionSupplier;

        private PatternOrError(@Nullable final Pattern pattern, final @Nullable Supplier<Exception> exceptionSupplier) {
            this.pattern = pattern;
            this.exceptionSupplier = exceptionSupplier;
        }

        static PatternOrError of(@Nullable final Pattern pattern) {return new PatternOrError( pattern, null );}

        static PatternOrError of(@Nullable final Supplier<Exception> exceptionSupplier) {return new PatternOrError( null, exceptionSupplier );}
    }

    ///  Error Context information for error handlers / loggers.
    @NullMarked
    public record ErrorContext(
            String messageID,                       // message we are trying to format.
            @Nullable String attributeID,           // message attribute (often null)
            Locale locale,                          // bundle Locale
            List<Exception> exceptions              // exceptions that occured during processing
    ) {
        /// If messageID is not known, use this
        public static final String UNSPECIFIED_PATTERN = "(Unspecified pattern)";
        /// If attributeID is missing or not required
        public static final String NO_ATTRIBUTE = "no attribute";


        public ErrorContext {
            requireNonNull( messageID );
            requireNonNull( locale );
            exceptions = List.copyOf( exceptions );
        }

        ///  friendly attribute name, or NO_ATTRIBUTE if not set
        public String attributeID() {
            return requireNonNullElse( attributeID, NO_ATTRIBUTE );
        }

        ///  Handles potentially null messageID, and labels it 'unknown pattern'
        public static ErrorContext of(@Nullable String msgID, @Nullable String attrID, Locale locale, List<Exception> exceptions) {
            return new ErrorContext(
                    requireNonNullElse( msgID, UNSPECIFIED_PATTERN ),
                    attrID,
                    locale,
                    exceptions
            );
        }
    }


    /// Fluent message formatting builder.
    ///
    /// A fluent, chainable helper created via [FluentBundle#fmt(String)] to format a single message
    /// or one of its attributes with optional variables and well-defined fallback behavior.
    ///
    /// Typical usage:
    /// {@snippet :
    /// // Basic message with variables
    ///
    /// String s1 = bundle.fmtBuilder("welcome")
    ///     .arg("user", "Ana")
    ///     .format();
    ///
    /// // Format an attribute
    /// String s2 = bundle.fmtBuilder("user-card")
    ///     .attribute("title")
    ///     .format();
    ///
    /// // Provide a constant fallback
    /// String s3 = bundle.fmtBuilder("price")
    ///     .arg("amount", 12)
    ///     .formatOrElse("N/A");
    ///
    /// // Provide a computed fallback
    /// String s4 = bundle.fmtBuilder("profile")
    ///     .arg("id", 42)
    ///     .formatOrElseGet(() -> computeDefaultProfile());
    ///
    /// // Convert rendering problems into your own exception
    /// String s5 = bundle.fmtBuilder("checkout")
    ///     .formatOrThrow(() -> new IllegalStateException("Cannot render checkout"));
    ///}
    ///
    /// Notes:
    /// - Fallback messages are not localized (though it is possible using a Supplier)
    /// - `arg` and `args` methods do not support null values.
    /// - Duplicate argument names overwrite previous values.
    /// - All terminal methods (`format`, `formatOrElseGet`, `formatOrElse`, `formatOrThrow`) will trigger rendering.
    /// - If a custom error consumer is configured on the bundle, all rendering exceptions and
    ///   fallback failures are reported via that consumer.
    @NullMarked
    public final class FmtBuilder {
        // Future considerations:
        //  - Fancier chaining? (e.g., with or(), that could attempt a localized format a different way)
        //  - Configurable fallback handlers? e.g., handler is invoked on error. Depending upon the error,
        //      format could be re-attempted or a fallback string could be provided. E.g., if the message
        //      key is missing a handler might provide a fallback string, but if a variable is missing
        //      the handler could just leave things as-is (for example!).

        private final String msgID;
        @Nullable private String attrID = null;
        private final Map<String, Object> args = new HashMap<>();

        private FmtBuilder(final String msgID) {
            this.msgID = msgID;
        }


        /// Use the given attribute ID for the message.
        /// If null, do not use an attributeID.
        ///
        /// @param attributeID attribute name to format; when null, the base message is used
        /// @return this builder for chaining
        public FmtBuilder attribute(final @Nullable String attributeID) {
            this.attrID = attributeID;
            return this;
        }

        /// Supply a single variable to the message. Null values are not supported.
        /// If the key `argName` already exists, it will be replaced.
        ///
        /// @param argName  variable name (non-null)
        /// @param argValue variable value (non-null)
        /// @return this builder for chaining
        public FmtBuilder arg(final String argName, final Object argValue) {
            requireNonNull( argName );
            requireNonNull( argValue );
            args.put( argName, argValue );
            return this;
        }

        /// Supply a map of arguments to be used in the message. This method may be called more than
        /// once, or in combination with [FmtBuilder#arg(String, Object)]. A duplicate key will replace
        /// an existing key, if present.
        ///
        /// @param argumentMap map of variable names to values (non-null)
        /// @return this builder for chaining
        public FmtBuilder args(Map<String, Object> argumentMap) {
            requireNonNull( argumentMap );
            args.putAll( argumentMap );
            return this;
        }

        /// Format the message.
        ///
        /// If exceptions occur during message rendering, use the String supplied here as a fallback.
        ///
        /// This method will also handle failure of the String supplier.
        ///
        /// This is a terminal operation.
        ///
        /// Exceptions will still be logged, including exceptions generated by the supplier, if so configured.
        ///
        /// @param fallbackSupplier supplier used to produce a fallback string when rendering fails (non-null)
        /// @return the rendered message, or the supplied fallback if rendering fails
        public String formatOrElseGet(final Supplier<String> fallbackSupplier) {
            requireNonNull( fallbackSupplier );

            final PatternOrError poe = patternOrError();

            if (poe.pattern == null) {
                assert poe.exceptionSupplier != null;
                return tryFallback( fallbackSupplier, null, poe.exceptionSupplier.get() );
            }

            // format the pattern.
            // unlike patternFormat(), we want to know about exceptions here, so we can attempt fallback
            final Scope scope = new Scope( FluentBundle.this, args );
            final List<FluentValue<?>> resolved = Resolver.resolvePattern( poe.pattern, scope );
            try {
                final String renderedMessage = registry.reduce( resolved, scope );
                if (scope.containsExceptions()) {
                    return tryFallback( fallbackSupplier, scope, null );
                } else {
                    // rendering success!
                    return renderedMessage;
                }
            } catch (FluentFunctionException e) {
                scope.addException( e );
                return tryFallback( fallbackSupplier, scope, e );
            }
        }

        /// Format the message.
        ///
        /// If exceptions occur during message rendering, use the String given here instead as a fallback.
        /// The exceptions will still be logged, if so configured.
        ///
        /// @param fallback fallback string to return when rendering fails (non-null)
        /// @return the rendered message, or the given fallback if rendering fails
        public String formatOrElse(final String fallback) {
            requireNonNull( fallback );
            return formatOrElseGet( () -> fallback );
        }


        /// Format the message.
        ///
        /// If exceptions occur during message rendering, throw the given Exception.
        ///
        /// Exceptions generated during message rendering will be logged, if so configured, but
        /// the supplied exception here will not be.
        ///
        /// @param exceptionSupplier supplier of exceptions
        /// @return the rendered message if no rendering errors occur
        /// @throws X supplied exception
        public <X extends Throwable> String formatOrThrow(final Supplier<? extends X> exceptionSupplier) throws X {
            requireNonNull( exceptionSupplier );

            final PatternOrError poe = patternOrError();

            if (poe.pattern == null) {
                assert poe.exceptionSupplier != null;
                customConsumeErr( null, List.of( poe.exceptionSupplier.get() ) );
                throw exceptionSupplier.get();
            }

            // format the pattern.
            // unlike patternFormat(), we want to know about exceptions here, so we can attempt fallback
            final Scope scope = new Scope( FluentBundle.this, args );
            final List<FluentValue<?>> resolved = Resolver.resolvePattern( poe.pattern, scope );
            try {
                final String renderedMessage = registry.reduce( resolved, scope );
                if (scope.containsExceptions()) {
                    throw exceptionSupplier.get();
                } else {
                    // rendering success!
                    return renderedMessage;
                }
            } catch (FluentFunctionException e) {
                scope.addException( e );
                throw exceptionSupplier.get();
            } finally {
                if (scope.containsExceptions()) {
                    customConsumeErr( scope, List.of() );
                }
            }
        }


        /// Render the message to a String. No fallback.
        ///
        /// This is a terminal operation.
        ///
        /// @return the rendered message string
        public String format() {
            if (attrID == null) {
                return FluentBundle.this.format( msgID, args );
            } else {
                return FluentBundle.this.format( msgID, attrID, args );
            }
        }

        private PatternOrError patternOrError() {
            if (attrID == null) {
                return getMessagePattern( msgID );
            } else {
                return getAttributePattern( msgID, attrID );
            }
        }


        // this must only be called by tryFormat(), for failure cases
        // if the fallback fails (e.g., supplier exception), we will handle that here
        private String tryFallback(final Supplier<String> fallbackSupplier, final @Nullable Scope scope, final @Nullable Exception cause) {
            List<Exception> causes = new ArrayList<>( 2 );
            if (cause != null) {
                causes.add( cause );
            }

            try {
                return fallbackSupplier.get();
            } catch (Exception e) {
                causes.add( new ResolutionException.FallbackFailure( e ) );
                final String attrDisplay = (attrID == null) ? "" : '.' + attrID;
                // the fallback for the fallback :)
                return String.format( "{Message '%s%s' fallback failure!}", msgID, attrDisplay );
            } finally {
                customConsumeErr( scope, causes );
            }
        }

        private void customConsumeErr(@Nullable final Scope scope, final List<Exception> exceptions) {
            if (errorConsumer != null) {
                List<Exception> list = new ArrayList<>( exceptions );

                if (scope != null) {
                    list.addAll( scope.exceptions() );
                }

                final ErrorContext context = ErrorContext.of( msgID, attrID, locale(), list );
                errorConsumer.accept( context );
            }
        }


    }


}
