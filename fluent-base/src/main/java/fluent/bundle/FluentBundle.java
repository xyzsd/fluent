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
package fluent.bundle;

import fluent.functions.*;
import fluent.functions.FluentImplicit.Implicit;
import fluent.syntax.AST.*;
import fluent.bundle.resolver.ReferenceException;
import fluent.bundle.resolver.Scope;
import fluent.types.DefaultFluentValueFactory;
import fluent.types.FluentValue;
import fluent.types.FluentValueFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// TODO: this class deserves the best documentation... we are not there yet
// TODO: also discuss 'global' options and usage
/**
 *  FluentBundle: This the primary class used for localization.
 *  <p>
 *
 *
 *
 *  <p>
 *      Example:
 *      <pre>
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
 *      </pre>
 *  </p>
 *
 *
 */
public class FluentBundle {

    private final Locale locale;
    private final boolean useIsolation;
    private final Map<String, Term> terms;
    private final Map<String, Message> messages;
    private final Map<String, FluentFunction> functions;
    private final EnumMap<Implicit, FluentImplicit> implicits;
    private final FluentValueFactory fluentValueCreator;
    private final FunctionResources fnResources;    // accessed only via Scope
    private final Options globalOpts;


    private FluentBundle(Builder b) {
        this.locale = b.locale;
        this.useIsolation = b.useIsolation;
        this.terms = Map.copyOf( b.terms );
        this.messages = Map.copyOf( b.messages );
        this.functions = Map.copyOf( b.functions );
        this.implicits = new EnumMap<>( b.implicits );
        this.fluentValueCreator = b.fluentValueCreator;
        this.fnResources = b.fnFactory.resources( b.locale  );
        this.globalOpts = b.globalOpts;
    }


    /**
     * Create a Builder to build a new FluentBundle.
     * <p>
     * build() will not throw an exception if this Builder is used, because all
     * required parameters are supplied.
     * </p>
     *
     * @param locale  Locale for the bundle to be built
     * @param factory Factory supplying the functions for the bundle to be built
     * @return Builder
     */
    public static Builder builder(@NotNull Locale locale, @NotNull FluentFunctionFactory factory) {
        Objects.requireNonNull( locale );
        Objects.requireNonNull( factory );
        return new Builder( locale, factory );
    }

    /**
     * Create a Builder to build a new FluentBundle.
     * <p>
     * No required components are set. However, build() will throw an exception if
     * a FluentFunctionFactory or Locale is not set.
     * </p>
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Create a Builder to build a new FluentBundle, using an existing FluentBundle as a base.
     */
    public static Builder builderFrom(@NotNull FluentBundle bundle) {
        return new Builder( Objects.requireNonNull( bundle ) );
    }

    /**
     * Bundle Locale
     */
    public Locale locale() {
        return locale;
    }

    /**
     * Bundle isolation setting
     */
    public boolean useIsolation() {
        return useIsolation;
    }

    /**
     * Bundle FluentValueCreator
     */
    public FluentValueFactory valueCreator() {
        return fluentValueCreator;
    }


    /**
     * Returns the Message for the given id
     */
    public Optional<Message> getMessage(@NotNull final String id) {
        return Optional.ofNullable( messages.get( id ) );
    }

    /**
     * Returns all Messages
     */
    public Map<String, Message> getMessages() {
        return messages;
    }

    /**
     * Returns the Term for the given id
     */
    public Optional<Term> getTerm(@NotNull final String id) {
        return Optional.ofNullable( terms.get( id ) );
    }

    /**
     * Returns all Terms
     */
    public Map<String, Term> getTerms() {
        return terms;
    }

    /**
     * Returns the Function for the given id
     */
    public Optional<FluentFunction> getFunction(@NotNull final String id) {
        return Optional.ofNullable( functions.get( id ) );
    }

    /**
     * Returns the implicit function
     */
    public @NotNull FluentImplicit implicit(@NotNull final FluentImplicit.Implicit id) {
        return implicits.get( id );
    }

    /**
     * Convenience method to get the Message Pattern for a given messageID.
     * <p>
     * A Message may not have a pattern if (and only if) the Message has attributes.
     * </p>
     *
     * @param messageID id
     * @return Pattern (if present)
     */
    public Optional<Pattern> getMessagePattern(@NotNull final String messageID) {
        return Optional.ofNullable( messages.get( messageID ) )
                .flatMap( Message::pattern );
    }

    /**
     * Convenience method to get the Pattern for an Attribute of a given Message.
     * <p>
     * This will return an empty Optional if the Message or Attribute cannot be found.
     * </p>
     *
     * @param messageID id
     * @return Pattern (if present)
     */
    public Optional<Pattern> getAttributePattern(@NotNull final String messageID, @NotNull final String attributeID) {
        return Optional.ofNullable( messages.get( messageID ) )
                .flatMap( msg -> Attribute.match( msg.attributes(), attributeID ) )
                .map( Attribute::pattern );
    }

    /**
     * Format the given pattern.
     * <p>
     * If an error occurs during format, an empty Optional is returned.
     * </p>
     *
     * @param pattern Pattern
     * @return formatted pattern
     */
    public Optional<String> formatPattern(@NotNull Pattern pattern) {
        return formatPattern( pattern, Map.of() );
    }

    /**
     * Format the given pattern, with the given arguments.
     * <p>
     * If an error occurs during format, an empty Optional is returned.
     * </p>
     *
     * @param pattern Pattern
     * @return formatted pattern
     */
    public Optional<String> formatPattern(@NotNull Pattern pattern, @NotNull Map<String, ?> args) {
        List<Exception> errors = new ArrayList<>( 4 );
        String result = patternFormat( pattern, args, errors );
        if (errors.isEmpty()) {
            return Optional.of( result );
        }
        return Optional.empty();
    }

    /**
     * Format the given pattern.
     * <p>
     * Errors will be placed in the errors list. The errors list MUST be a mutable (writable) list.
     * As much of the message that can be formatted, will be formatted, if errors occur.
     * </p>
     *
     * @param pattern Pattern
     * @return formatted pattern
     */
    public String formatPattern(@NotNull Pattern pattern, @NotNull List<Exception> errors) {
        return patternFormat( pattern, Map.of(), errors );
    }

    /**
     * Format the given pattern, with the given arguments.
     * <p>
     * Errors will be placed in the errors list. The errors list MUST be a mutable (writable) list.
     * As much of the message that can be formatted, will be formatted, if errors occur.
     * </p>
     * <p>
     * For example, given the Message 'mymessage' in an FTL definition: {@code "mymessage = Hello there, {$name}"}
     *     <ul>
     *         <li>
     *             {@code formatPattern(..., Map.of("name","Billy"), ...)} result: {@code "Hello there, Billy"}
     *         </li>
     *         <li>
     *             {@code formatPattern(..., Map.of(), ...)} result: {@code "Hello there, {$name}"}, indicating
     *             an error with the "$name" placeable.
     *         </li>
     *     </ul>
     *
     * @param pattern Pattern
     * @return formatted pattern
     */
    public String formatPattern(@NotNull Pattern pattern, @NotNull Map<String, ?> args, @NotNull List<Exception> errors) {
        return patternFormat( pattern, args, errors );
    }



    /**
     *  The simplest format() method.
     *  <p>
     *      Gets the message with the given name, and displays it. If there is an error,
     *      the error message is output instead.
     *  </p>
     *
     *  @param messageID The name of the message to format.
     * @return the formatted message.
     */
    public String format(@NotNull String messageID) {
        return format(messageID, Map.of());
    }

    /**
     * Simple message format.
     *  <p>
     *      Gets the message with the given name, and displays it. If there is an error,
     *      the error message is output instead.
     *  </p>
     *
     * @param messageID message name to format
     * @param args argument map (name-value pairs)
     * @return the formatted message.
     */
    public String format(@NotNull String messageID, @NotNull Map<String, ?> args) {
        List<Exception> errors = new ArrayList<>( 4 );
        return getMessagePattern(messageID)
                .map( pattern -> patternFormat(pattern, args,  errors ) )
                .orElse( "Unknown messageID '"+messageID+"'" );
    }


    ////////////////////////////////
    // private
    ////////////////////////////////

    private String patternFormat(@NotNull Pattern pattern, @NotNull Map<String, ?> args, @NotNull List<Exception> errors) {
        final Scope scope = new Scope( this, this.fnResources, args, errors, globalOpts );
        final List<FluentValue<?>> resolved = pattern.resolve( scope );
        return scope.reduce( resolved );
    }


    ////////////////////////////////
    // Builder
    ////////////////////////////////

    /**
     * Build a FluentBundle
     * <p>
     * Only a Locale is required for a the builder to successfully build a FluentBundle, though such a created
     * bundle would not have any useful functionality.
     * </p>
     * <p>
     * If Functions are to be added/changed/removed outside of a FluentFunctionFactory, the factory should
     * be set prior to changing functions.
     * </p>
     * <p>
     * Builders can be re-used. Builders are not guaranteed to be threadsafe.
     * </p>
     */
    public static final class Builder {

        Locale locale;
        FluentFunctionFactory fnFactory;
        Map<String, Term> terms = new HashMap<>();
        Map<String, Message> messages = new HashMap<>();
        Map<String, FluentFunction> functions = new HashMap<>();
        Map<Implicit, FluentImplicit> implicits = new HashMap<>();
        FluentValueFactory fluentValueCreator = DefaultFluentValueFactory.create();
        boolean useIsolation = false;
        Options globalOpts = Options.EMPTY;

        private Builder() {

        }


        // Builder with Locale & Function Factory
        private Builder(@NotNull Locale locale, @NotNull FluentFunctionFactory factory) {
            this.locale = locale;
            this.fnFactory = factory;
            mergeFunctions();
        }

        // Builder from existing bundle
        private Builder(final FluentBundle from) {
            this.locale = from.locale;
            this.useIsolation = from.useIsolation;
            this.terms = new HashMap<>( from.terms );
            this.messages = new HashMap<>( from.messages );
            this.functions = new HashMap<>( from.functions );
            this.implicits = new EnumMap<>( from.implicits );
            this.fluentValueCreator = from.fluentValueCreator;
            // FunctionResources: local to each bundle
            this.globalOpts = from.globalOpts;
        }


        /**
         * Use the given Locale when creating this bundle.
         * <p>
         * A Locale is required for a bundle to be created
         * without error.
         *
         * @param locale Locale
         * @return Builder
         */
        public Builder withLocale(@NotNull Locale locale) {
            this.locale = Objects.requireNonNull( locale );
            return this;
        }

        /**
         * Use the given FluentFunctionFactory when creating this bundle.
         * <p>
         * A FluentFunctionFactory is required for a bundle to be created
         * without error.
         *
         * @param factory FluentFunctionFactory
         * @return Builder
         */
        public Builder withFunctionFactory(@NotNull FluentFunctionFactory factory) {
            this.fnFactory = Objects.requireNonNull( factory );
            mergeFunctions();
            return this;
        }

        /**
         * Set whether this builder should use unicode isolating characters
         */
        public Builder withIsolation(boolean value) {
            useIsolation = value;
            return this;
        }

        /**
         * Set the FluentValueCreator for the Builder
         *
         * @param creator FluentValueCreator
         * @return Builder
         */
        public Builder withValueCreator(@NotNull FluentValueFactory creator) {
            fluentValueCreator = Objects.requireNonNull( creator );
            return this;
        }


        /**
         * Add the given resource.
         *
         * @param resource FluentResource to add
         * @return Builder
         * @throws ReferenceException if term or message entry already exists
         */
        public Builder addResource(@NotNull final FluentResource resource) {
            return addResource( resource, true );
        }


        /**
         * Add the given resource.
         * <p>
         * Terms and Messages added will replace existing Terms or Messages with the
         * same name.
         * </p>
         *
         * @param resource FluentResource to add
         * @return Builder
         */
        public Builder addResourceOverriding(@NotNull final FluentResource resource) {
            return addResource( resource, false );
        }


        private Builder addResource(@NotNull final FluentResource resource, final boolean checkClash) {
            Objects.requireNonNull( resource );

            List<String> clashes = new ArrayList<>();

            for (Entry entry : resource.entries()) {
                if (entry instanceof Message msg) {
                    final Message existing = messages.putIfAbsent( msg.name(), msg );
                    if (checkClash && existing != null) {
                        clashes.add( msg.name() );
                    }
                } else if (entry instanceof Term term) {
                    final Term existing = terms.putIfAbsent( term.name(), term );
                    if (checkClash && existing != null) {
                        clashes.add( '-' + term.name() );
                    }
                }
            }

            if (!clashes.isEmpty()) {
                throw ReferenceException.duplicateEntry( String.join( ", ", clashes ) );
            }

            return this;
        }


        /**
         * Add or Replace a single FluentFunction
         * <p>
         * If multiple functions are to be initialized, consider initialization via a FluentFunction.Factory
         * instead. Implicit functions may NOT be altered by this method.
         * </p>
         *
         * @param fn Initialized FluentFunction
         * @throws NullPointerException if FluentFunction is null
         */
        public Builder addFunction(@NotNull final FluentFunction fn) {
            Objects.requireNonNull( fn );
            functions.put( fn.name(), fn );
            return this;
        }

        /**
         * Remove a FluentFunction by name.
         * <p>
         * If the named function does not exist (or has already been removed), there is no effect or error.
         * </p>
         *
         * @throws NullPointerException if name is null
         */
        public Builder removeFunction(@NotNull final String name) {
            functions.remove( Objects.requireNonNull( name ) );
            return this;
        }

        /**
         * Set an Implicit function.
         */
        public Builder setImplicit(@NotNull final FluentImplicit fn) {
            Objects.requireNonNull( fn );

            if (!fn.id().toString().equals( fn.name() )) {
                throw new IllegalArgumentException( "Implicit function name/identifier mismatch: " + fn.name() + ":" + fn.id() );
            }

            implicits.put( fn.id(), fn );
            return this;
        }

        /**
         * Set global options / default options, that apply to all functions.
         */
        public Builder setGlobalOptions(@NotNull Options options) {
            this.globalOpts = Objects.requireNonNull( options );
            return this;
        }


        /**
         * Create the FluentBundle.
         *
         * @return a new FluentBundle
         */
        public FluentBundle build() {
            Objects.requireNonNull( locale, "Locale not set." );
            Objects.requireNonNull( fnFactory, "FluentFunctionFactory not set." );
            if (implicits.size() != Implicit.values().length) {
                throw new IllegalStateException(
                        String.format(
                                "Only %d implicits were set; %d required",
                                implicits.size(), Implicit.values().length )
                );
            }

            // map implicits into the 'regular' function map
            // implicits will (and must!) override any non-implicit registered function with same name
            implicits.forEach( (key, value) -> functions.put( key.name(), value ) );

            return new FluentBundle( this );
        }


        private void mergeFunctions() {
            fnFactory.functions().forEach(
                    fn -> functions.merge( fn.name(), fn, (vOld, vNew) -> vOld )
            );

            fnFactory.implicits().forEach(
                fn -> implicits.merge( fn.id(), fn, (vOld, vNew) -> vOld )
            );

        }
    }


}
