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

import fluent.bundle.resolver.FluentValueFormatter;
import fluent.bundle.resolver.Resolver;
import fluent.functions.*;
import fluent.functions.FluentImplicit.Implicit;
import fluent.functions.list.reducer.ListFn;
import fluent.functions.numeric.NumberFn;
import fluent.functions.temporal.TemporalFn;
import fluent.syntax.AST.*;
import fluent.bundle.resolver.ReferenceException;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;

import org.jspecify.annotations.NullMarked;

import java.time.temporal.TemporalAccessor;
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
 *
 *
 */
@NullMarked
public class FluentBundle {

    private final Locale locale;
    private final boolean useIsolation;
    private final Map<String, Term> terms;
    private final Map<String, Message> messages;

    private final Map<String, FluentFunction> functions;
    // TODO: implicit formatters should be derived from functions (probably) or set explicitly
    private final PluralSelector pluralSelector;    // accessed only via Scope
    private final FluentValueFormatter formatter;   // accessed only via Scope

    private final Options globalOpts;


    private FluentBundle(Builder b) {
        this.locale = b.locale;
        this.useIsolation = b.useIsolation;
        this.terms = Map.copyOf( b.terms );
        this.messages = Map.copyOf( b.messages );
        this.functions = Map.copyOf( b.functions );
        this.pluralSelector = b.pluralSelector;
        this.formatter = b.formatter;
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
     * @return Builder
     */
    public static Builder builder(Locale locale) {
        Objects.requireNonNull( locale );
        return new Builder( locale );
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
    public static Builder builderFrom(FluentBundle bundle) {
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
     * Returns the Message for the given id
     */
    public Optional<Message> getMessage(final String id) {
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
    public Optional<Term> getTerm(final String id) {
        return Optional.ofNullable( terms.get( id ) );
    }

    /**
     * Returns all Terms
     */
    public Map<String, Term> getTerms() {
        return terms;
    }


    ///  Return the Function for the given ID
    public Optional<FluentFunction> function(final String id) {
        return Optional.ofNullable( functions.get( id ) );
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
    public Optional<Pattern> getMessagePattern(final String messageID) {
        return Optional.ofNullable( messages.get( messageID ) )
                .flatMap( m -> Optional.ofNullable( m.pattern()) );
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
    public Optional<Pattern> getAttributePattern(final String messageID, final String attributeID) {
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
    public Optional<String> formatPattern(Pattern pattern) {
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
    public Optional<String> formatPattern(Pattern pattern, Map<String, ?> args) {
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
    public String formatPattern(Pattern pattern, List<Exception> errors) {
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
    public String formatPattern(Pattern pattern, Map<String, ?> args, List<Exception> errors) {
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
    public String format(String messageID) {
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
    public String format(String messageID, Map<String, ?> args) {
        List<Exception> errors = new ArrayList<>( 4 );
        return getMessagePattern(messageID)
                .map( pattern -> patternFormat(pattern, args,  errors ) )
                .orElse( "Unknown messageID '"+messageID+"'" );
    }



    private String patternFormat(Pattern pattern, Map<String, ?> args, List<Exception> errors) {
        final Scope scope = new Scope( this, pluralSelector, formatter, args, errors, globalOpts );
        final List<FluentValue<?>> resolved = Resolver.resolve( pattern, scope );
        return scope.reduce( resolved );
    }



    /**
     * Build a FluentBundle
     * <p>
     * If required parameters are not set, an exception will be thrown during build.
     * <p>
     * If Functions are to be added, replaced, or removed outside of a FluentFunctionFactory, the factory must
     * be set in this Builder prior to adding/replacing/removing functions.
     * <p>
     * Builders can be re-used. Builders are not guaranteed to be threadsafe.
     * </p>
     */
    public static final class Builder {
        final FluentValueFormatter.Builder fvfBuilder;
        Locale locale;
        Map<String, Term> terms = new HashMap<>();
        Map<String, Message> messages = new HashMap<>();
        Map<String, FluentFunction> functions = new HashMap<>();
        boolean useIsolation = false;
        PluralSelector pluralSelector;
        FluentValueFormatter formatter;
        Options globalOpts = Options.EMPTY;

        private Builder() {
            this.fvfBuilder = FluentValueFormatter.builder();
        }

        // Builder with Locale & Function Factory
        private Builder(Locale locale) {
            this.fvfBuilder = FluentValueFormatter.builder();
            this.locale = locale;
        }

        // Builder from existing bundle
        private Builder(final FluentBundle from) {
            this.fvfBuilder = FluentValueFormatter.builder();
            this.locale = from.locale;
            this.useIsolation = from.useIsolation;
            this.terms = new HashMap<>( from.terms );
            this.messages = new HashMap<>( from.messages );
            this.functions = new HashMap<>( from.functions );
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
        public Builder withLocale(Locale locale) {
            this.locale = Objects.requireNonNull( locale );
            return this;
        }

        /**
         * Set whether this builder should use unicode isolating characters
         */
        public Builder withIsolation(boolean value) {
            useIsolation = value;
            return this;
        }


        /// Add default functions from FluentFunctions.
        public Builder withDefaultFunctions() {
            FluentFunctions.IMPLICITS.functions()
                    .forEach( fn -> functions.put(fn.name(), fn) );

            fvfBuilder.setTerminalReducer( ListFn.LIST );
            fvfBuilder.setNumberFormatter( NumberFn.NUMBER, Options.EMPTY );
            fvfBuilder.setTemporalFormatter( TemporalFn.TEMPORAL, Options.EMPTY );

            // TODO: add the others when ready
            System.err.println("--TODO-- add additional fn when ready");

            return this;
        }

        /**
         * Add the given resource.
         *
         * @param resource FluentResource to add
         * @return Builder
         * @throws ReferenceException if term or message entry already exists
         */
        public Builder addResource(final FluentResource resource) {
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
        public Builder addResourceOverriding(final FluentResource resource) {
            return addResource( resource, false );
        }


        private Builder addResource(final FluentResource resource, final boolean checkClash) {
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
        public Builder addFunction(final FluentFunction fn) {
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
        public Builder removeFunction(final String name) {
            functions.remove( Objects.requireNonNull( name ) );
            return this;
        }


        /**
         * Set global options / default options, that apply to all functions.
         */
        public Builder setGlobalOptions(Options options) {
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

            // plural selector
            pluralSelector = new PluralSelector(locale);

            // map implicits into the 'regular' function map
            // implicits will (and must!) override any non-implicit registered function with same name
            // TODO: re-look at this
            //implicits.forEach( (key, value) -> functions.put( key.name(), value ) );

            formatter = fvfBuilder.build();

            return new FluentBundle( this );
        }


   }


}
