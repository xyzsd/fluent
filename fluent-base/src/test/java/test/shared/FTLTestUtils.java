/*
 *
 *  Copyright (c) 2025, xyzsd (Zach Del)
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
 *
 */

package test.shared;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.bundle.resolver.Scope;
import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import fluent.function.ResolvedParameters;
import fluent.function.functions.DefaultFunctionFactories;
import fluent.syntax.AST.*;
import fluent.syntax.parser.FTLParser;
import fluent.syntax.parser.FTLParseException;
import fluent.types.FluentString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FTLTestUtils {

    /// Error Logger for testing (to stderr)
    static final Consumer<FluentBundle.ErrorContext> TEST_ERROR_LOGGER = (ec) -> {
        System.err.printf( "ERROR encountered during formatting of message '%s' (%s), locale %s; [%d errors]:\n",
                ec.messageID(), ec.attributeID(), ec.locale(), ec.exceptions().size() );
        ec.exceptions().forEach( e -> System.err.println( "  " + e ) );
    };


    private FTLTestUtils() {}

    public static FluentResource parseFile(String fileName) throws IOException {
        requireNonNull( fileName );
        System.out.println( "Input FTL: " + fileName );

        final FTLParser.Implementation implementation = FTLParser.Implementation.SCALAR;

        return FTLParser.parse(
                Thread.currentThread().getContextClassLoader(),
                fileName,
                FTLParser.ParseOptions.EXTENDED,
                implementation );
    }


    // useful for debugging resource file
    @SuppressWarnings( "unused" )
    public static void show(FluentResource resource) {
        System.out.println( "BEGIN" );
        resource.errors().forEach( System.out::println );
        System.out.println( "-----" );
        resource.entries().forEach( System.out::println );
        System.out.println( "-----" );
        resource.junk().forEach( System.out::println );
        System.out.println( "END\n" );
    }


    // search resource for the given errorcode-line exception
    public static boolean matchParseException(final FluentResource resource, final FTLParseException.ErrorCode errorCode, final int line) {
        final boolean match = resource.errors()
                .stream()
                .anyMatch( e -> (e.errorCode() == errorCode) && (e.line() == line) );
        if (!match) {
            System.err.println( "EXPECTED ParseException NOT FOUND: " + errorCode + " @ line " + line );
        }
        return match;
    }

    // bundle from string
    @SuppressWarnings( "unused" )
    public static FluentBundle bundleFromString(final String in) {
        final FluentResource resource = FTLParser.parse( in, FTLParser.ParseOptions.EXTENDED,
                FTLParser.Implementation.SCALAR );
        return basicBundleSetup( resource, false );
    }

    // basic bundle setup -- only the required functions
    public static FluentBundle basicBundleSetup(final FluentResource resource, boolean withErrorLogger) {
        requireNonNull( resource, "FluentResource cannot be null" );

        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .build();

        // a 'null' logger is allowed
        final Consumer<FluentBundle.ErrorContext> errorLogger = withErrorLogger ? TEST_ERROR_LOGGER : null;

        return FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .withLogger( errorLogger )
                .build();
    }

    ///  extended bundle setup -- all functions
    public static FluentBundle extendedBundleSetup(final FluentResource resource, boolean withErrorLogger) {
        requireNonNull( resource, "FluentResource cannot be null" );

        final FluentFunctionRegistry.Builder regBuilder = FluentFunctionRegistry.builder();
        DefaultFunctionFactories.allNonImplicits().forEach( regBuilder::addFactory );
        final FluentFunctionRegistry registry = regBuilder.build();

        // a 'null' logger is allowed
        final Consumer<FluentBundle.ErrorContext> errorLogger = withErrorLogger ? TEST_ERROR_LOGGER : null;

        return FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .withLogger( errorLogger )
                .build();
    }

    public static String fmt(final FluentBundle bndl, final String msgID) {
        return fmt( bndl, msgID, Map.of() );
    }

    public static String patternFormat(final FluentBundle bndl, final Pattern pattern) {
        assertNotNull( bndl );
        assertNotNull( pattern );
        return bndl.patternFormat( pattern, Map.of() );
    }

    public static String attr(final FluentBundle bndl, final String msgID, final String attrib) {
        assertNotNull( bndl );
        assertNotNull( msgID );
        assertNotNull( attrib );

        final String formatted = bndl.format( msgID, attrib );
        System.out.println( "attr(" + msgID + "." + attrib + ") '" + formatted + "'" );
        return formatted;
    }

    public static String fmt(final FluentBundle bndl, final String msgID, Map<String, Object> args) {
        assertNotNull( bndl );
        assertNotNull( msgID );
        assertNotNull( args );

        final String formatted = bndl.format( msgID, args );
        System.out.println( "fmt(" + msgID + ") '" + formatted + "'" );
        return formatted;
    }


    public static String term(final FluentBundle bndl, final String termID, Map<String, Object> args) {
        assertNotNull( bndl );
        assertNotNull( termID );
        assertNotNull( args );

        String formatted = bndl.term( termID )
                .map( Term::value )
                .map( pattern -> bndl.patternFormat( pattern, args ) )
                .orElseThrow( () -> new IllegalArgumentException( "term(" + termID + ") not found." ) );
        System.out.println( "term(" + termID + ") '" + formatted + "'" );
        return formatted;
    }

    public static String termAttr(final FluentBundle bndl, final String termID, final String attrID, Map<String, Object> args) {
        assertNotNull( bndl );
        assertNotNull( termID );
        assertNotNull( attrID );
        assertNotNull( args );

        String formatted = bndl.term( termID )
                .flatMap( term -> term.attribute( attrID ) )
                .map( Attribute::pattern )
                .map( pattern -> bndl.patternFormat( pattern, args ) )
                .orElseThrow( () -> new IllegalArgumentException( "term(" + termID + "." + attrID + ") not found." ) );
        System.out.println( "term(" + termID + "." + attrID + ") '" + formatted + "'" );
        return formatted;
    }


    // get the 1st selector found for a given message, but we only go 1 level deep
    public static SelectExpression getSelectExpression(final FluentBundle bndl, final String msgID) {
        assertNotNull( bndl );
        assertNotNull( msgID );

        final Message message = bndl.message( msgID ).orElseThrow( () -> new IllegalArgumentException( "message(" + msgID + ") not found." ) );
        if (message.pattern() == null) {
            throw new IllegalArgumentException( "message(" + msgID + "): no pattern." );
        }

        SelectExpression selectExpression = null;
        for (PatternElement element : message.pattern().elements()) {
            if (element instanceof PatternElement.Placeable(Expression expression)
                    && expression instanceof SelectExpression se) {
                selectExpression = se;
                break;
            }
        }

        if (selectExpression == null) {
            throw new IllegalArgumentException( "message(" + msgID + "): no (non-nested) select expression found." );
        }

        return selectExpression;
    }


    /// A function, for testing ONLY, that allows us to evaluate
    /// Function arguments. This function must not be cached, and
    /// we will set factory arguments prior to creation so we can
    ///  verify the arguments. This test function IS NOT threadsafe
    @NullMarked
    public static class FnFactory implements FluentFunctionFactory<FluentFunction.Transform> {

        private final String name;
        private @Nullable BiConsumer<ResolvedParameters, Scope> transform = null;
        private @Nullable Consumer<Options> creator = null;
        private @Nullable String result = null;

        ///  Register the FnFactory, bound to the given name.
        public FnFactory(final String name) {
            this.name = requireNonNull( name );
        }

        public synchronized void reset() {
            transform = null;
            creator = null;
            result = null;
        }

        ///  REQUIRED: set the returned result
        public synchronized void setResult(String result) {
            this.result = requireNonNull( result );
        }

        ///  REQUIRED: set the transform-checker (check parameters) REQUIRED
        public synchronized void setTransform(BiConsumer<ResolvedParameters, Scope> transform) {
            this.transform = requireNonNull( transform );
        }

        ///  OPTIONAL: set 'creator' which can observe creation and check options
        @SuppressWarnings( "unused" )
        public synchronized void setCreator(Consumer<Options> creator) {
            this.creator = requireNonNull( creator );
        }

        @Override
        public synchronized FluentFunction.Transform create(Locale locale, Options options) {
            if (creator != null) {
                creator.accept( options );
                creator = null;
            }

            if (transform == null) {
                throw new IllegalStateException( "inject: transform not set OR already triggered" );
            } else if (result == null) {
                throw new IllegalStateException( "inject: result not set OR already used" );
            } else {
                final BiConsumer<ResolvedParameters, Scope> transferredTransform = transform;
                transform = null;
                return (parameters, scope) -> {
                    transferredTransform.accept( parameters, scope );
                    return List.of( FluentString.of( result ) );
                };
            }
        }


        @Override
        public boolean canCache() {
            return false;
        }

        @Override
        public String name() {
            return name;
        }
    }
}
