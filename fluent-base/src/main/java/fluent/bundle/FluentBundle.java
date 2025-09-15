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

import fluent.bundle.resolver.Resolver;
import fluent.bundle.resolver.Scope;
import fluent.function.FluentFunctionException;
import fluent.function.Options;
import fluent.syntax.AST.*;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static fluent.bundle.resolver.ResolutionException.ReferenceException;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/// FluentBundle: This the primary class used for localization.
// TODO: document well!, convert any non-markdown comments to markdown
/* OLD example: need to update
 *      {@code
 *
 *          String in = Files.readString( Path.of( "mydir/myfile.ftl" ) );
 *
 *          FluentResource parse = FTLParser.parse( FTLStream.of( in ) );
 *
 *          FluentBundle bundle = FluentBundle.builder( Locale.US, CLDRFunctionFactory.INSTANCE )
 *                                            .addResource( resource )
 *                                            .build();
 *
 *          // assumes the following "myfile.ftl" contains the message:
 *          //      helloMessage = "Hello there, {name}!"
 *
 *          String myName = "Billy";
 *          String output = bundle.format("helloMessage",Map.of("name", myName));
 *
 *          System.out.println(output);     // "Hello there, Billy!"
 *
 *      }
 */
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
    public static Builder builderFrom(FluentBundle bundle, FluentFunctionCache cache) {
        return new Builder( bundle, cache );
    }

    /// error text for unknown message IDs
    private static String unknownMessage(final String msgID) {
        return "{Unknown message: '" + msgID + "'}";
    }

    /// error text for unknown attribute IDs
    private static String unknownAttribute(final String msgID, final String attributeID) {
        return "{Unknown attribute '" + attributeID + "' for message '" + msgID + "'}";
    }

    /// error text for messages without patterns
    private static String unknownPattern(final String msgID) {
        return "{No pattern specified for message: '" + msgID + "'}";
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

        final Message message = messages.get( messageID );

        if (message == null) {
            consumeError( messageID, null, () -> ReferenceException.unknownMessageOrAttribute( messageID, null ) );
            return unknownMessage( messageID );
        } else if (message.pattern() == null) {
            consumeError( messageID, null, () -> ReferenceException.noValue( messageID ) );
            return unknownPattern( messageID );
        } else {
            return patternFormat( message.pattern(), args, messageID, null );
        }
    }

    ///  Get the Pattern for a Message. Errors logged if not found, null if absent.
    private @Nullable Pattern getMessagePattern(final String messageID) {
        requireNonNull( messageID );

        final Message message = messages.get( messageID );

        if (message == null) {
            consumeError( messageID, null, () -> ReferenceException.unknownMessageOrAttribute( messageID, null ) );
        } else if (message.pattern() == null) {
            consumeError( messageID, null, () -> ReferenceException.noValue( messageID ) );
        } else {
            return message.pattern();
        }

        return null;
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

        final Message message = messages.get( messageID );
        if (message != null) {
            final Attribute attribute = message.attribute( attributeID );
            if (attribute != null) {
                return patternFormat( attribute.pattern(), args, messageID, attributeID );
            }
        }

        // error: either Message or Attribute is missing
        consumeError( messageID, null, () -> ReferenceException.unknownMessageOrAttribute( messageID, attributeID ) );
        return unknownAttribute( messageID, attributeID );
    }

    ///  Get the Pattern for an Attribute
    private @Nullable Pattern getAttributePattern(final String messageID, final String attributeID) {
        requireNonNull( messageID );
        requireNonNull( attributeID );

        final Message message = messages.get( messageID );
        if (message != null) {
            final Attribute attribute = message.attribute( attributeID );
            if (attribute != null) {
                return attribute.pattern();
            }
        }

        // error: either Message or Attribute is missing
        consumeError( messageID, null, () -> ReferenceException.unknownMessageOrAttribute( messageID, attributeID ) );
        return null;
    }

    ///  The simplest format() method.
    ///
    ///  Gets the message with the given name, and displays it. If there is an error,
    ///  the error message is output instead.
    ///
    ///  This method should not throw in normal usage
    ///
    /// @param messageID The name of the message to format.
    /// @return the formatted message.
    /// @throws NullPointerException if messageID is null
    public String format(String messageID) {
        return format( messageID, Map.of() );
    }

    ///  The simplest format() method for attributes.
    ///
    ///  Gets the message with the given name, and displays it. If there is an error,
    ///  the error message is output instead.
    ///
    ///  This method should not throw in normal usage
    ///
    /// @param messageID   Message ID
    /// @param attributeID Attribute ID containing the pattern to format.
    /// @return the formatted message.
    /// @throws NullPointerException if messageID or attributeID
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
    // TODO: finalize
    public FmtBldr fmtBuilder(final String messageID) {
        return new FmtBldr( messageID );
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
        public Builder withLocale(final Locale locale) {
            this.locale = requireNonNull( locale );
            return this;
        }

        /// Set whether this builder should use Unicode isolating characters
        ///
        /// For more information, see:
        ///
        /// [Project Fluent](https://github.com/projectfluent/fluent.js/wiki/Unicode-Isolation)
        /// [unicode.org](https://unicode.org/reports/tr9/#Directional_Formatting_Characters)
        /// [W3C](https://www.w3.org/International/articles/inline-bidi-markup/#nomarkup)
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
        /// @throws RuntimeException if term or message entry already exists
        public Builder addResource(final FluentResource resource) {
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
                // TODO: perhaps custom exception rather than Runtime
                throw new RuntimeException( "Duplicate Messages or Terms: " +
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
        /// @param resource FluentResource to add
        /// @return Builder
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
        public Builder withRegistry(final FluentFunctionRegistry registry) {
            this.registry = requireNonNull( registry );
            return this;
        }

        ///  Set the function cache
        ///
        /// This replaces the existing FluentFunctionCache. A FluentBundle can have only a
        /// single FluentFunctionCache.
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
        ///        // all calls to STRINGSORT() will be reversed, unless "order" is specified otherwise
        ///        FluentBundle.Builder builder;
        ///        // ...
        ///        builder.withFunctionOptions("STRINGSORT", Options.of("order","reversed"));
        ///        // ...
        ///}
        ///
        public Builder withFunctionOptions(final String functionName, final Options options) {
            requireNonNull( functionName );
            requireNonNull( options );

            if (!registry.contains( functionName )) {
                throw new IllegalArgumentException( String.format(
                        """
                                Cannot find the function factory named '%s';\
                                check spelling and make sure this was set in FluentFunctionRegistry.
                                """, functionName ) );
            }

            if (options == Options.EMPTY) {
                defaultOptions.remove( functionName );
            } else {
                defaultOptions.put( functionName, options );
            }

            return this;
        }

        ///  Add a logger, that will log any Scope exceptions.
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

    ///  Error Context information for error handlers / loggers.
    @NullMarked
    public record ErrorContext(
            String messageID,                       // message we are trying to format.
            @Nullable String attributeID,           // message attribute (often null)
            Locale locale,                          // bundle Locale
            List<Exception> exceptions              // exceptions that occured during processing
    ) {
        /// If messageID is not known, use this
        public static final String UNSPECIFIED_PATTERN = "(Unspecified Pattern)";
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

    /// Format Builder: **NOTE: this class is experimental**
    public final class FmtBldr {
        private final String msgID;
        private @Nullable String attrID = null;
        private Map<String, Object> args = Map.of();

        private FmtBldr(final String msgID) {
            this.msgID = msgID;
        }

        public FmtBldr attribute(final @Nullable String attributeID) {
            this.attrID = attributeID;
            return this;
        }

        // useful for when there is ONE argument; replaces any existing argument/arguments
        public FmtBldr argument(final String argName, final Object argValue) {
            this.args = Map.of( argName, argValue );
            return this;
        }

        // for multiple arguments. will replace any existing arguments if present or called again
        public FmtBldr arguments(Map<String, Object> argumentMap) {
            this.args = Map.copyOf( argumentMap );
            return this;
        }


        public String orElseGet(final Supplier<String> fallback) {
            final String fmt = tryFormat();
            return (fmt == null)
                    ? fallback.get()
                    : fmt;
        }

        public String orElse(final String fallback) {
            final String fmt = tryFormat();
            return (fmt == null)
                    ? fallback
                    : fmt;
        }

        public <X extends Throwable> String orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
            final String fmt = tryFormat();
            if (fmt == null) {
                throw exceptionSupplier.get();
            }
            return fmt;
        }

        public String format() {
            if (attrID == null) {
                return FluentBundle.this.format( msgID, args );
            } else {
                return FluentBundle.this.format( msgID, attrID, args );
            }
        }


        private @Nullable String tryFormat() {
            final Pattern pattern = (attrID == null)
                    ? getMessagePattern( msgID )
                    : getAttributePattern( msgID, attrID  );

            if (pattern == null) {
                //getAttributePattern / getMessagePattern logs an error if pattern not found
                return null;
            }

            final Scope scope = new Scope( FluentBundle.this, args );
            final List<FluentValue<?>> resolved = Resolver.resolvePattern( pattern, scope );
            String formatted = null;
            try {
                formatted = registry.reduce( resolved, scope );
            } catch (FluentFunctionException e) {
                scope.addException( e );
            } finally {
                consumeError( msgID, attrID, scope );
            }

            return scope.containsExceptions() ? null : formatted;
        }

    }
}
