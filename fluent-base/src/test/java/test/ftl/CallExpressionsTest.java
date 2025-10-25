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

package test.ftl;import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import fluent.types.FluentError;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import static fluent.syntax.parser.FTLParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CallExpressionsTest {

    static final String RESOURCE = "fixtures/call_expressions.ftl";
    static FluentResource resource;
    static FluentBundle bundle;
    static FUNFactory funFactory;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        funFactory = new FUNFactory();

        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .addFactory( funFactory )
                .build();

        bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                // no logger .withLogger( FTLTestUtils.TEST_ERROR_LOGGER )
                .build();
    }



    @Test
    public void verifyExceptions() {
        // some exceptions can occur during parsing, depending upon the input;
        // verify any exceptions that were raised are indeed present.

        assertEquals( 9, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 8 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0008, 10 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0008, 12 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 14 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0021, 24 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0022, 27 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 98 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 99 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 100 ) );

    }

    @Test
    public void validNames() {
        assertEquals(
                "{Unknown function: FUN1()}",
                FTLTestUtils.fmt( bundle, "valid-func-name-01" )
        );
        assertEquals(
                "{Unknown function: FUN_FUN()}",
                FTLTestUtils.fmt( bundle, "valid-func-name-02" )
        );
        assertEquals(
                "{Unknown function: FUN-FUN()}",
                FTLTestUtils.fmt( bundle, "valid-func-name-03" )
        );
    }

    @Test
    public void Arguments() {
        // positionals
        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 3, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals( 1, rp.positional( 1 ).size() );
            assertEquals( 1, rp.positional( 2 ).size() );

            assertEquals(
                    FluentNumber.of(1),
                    rp.positional( 0 ).getFirst()
            );

            assertEquals(
                    FluentString.of("a"),
                    rp.positional( 1 ).getFirst()
            );

            // not a literal, and 'msg' is unknown
            assertEquals(
                    FluentError.of( "{Unknown message: 'msg'}" ),
                    rp.positional( 2 ).getFirst()
            );

            return List.of( FluentString.of( "true_1" ) );
        } );
        assertEquals(
                "true_1",
                FTLTestUtils.fmt(bundle, "positional-args")
        );

        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertEquals( 1, options.asInt( "x" ).getAsInt() );
            assertEquals( "Y", options.asString( "y" ).get() );
        } );
        funFactory.setInjected( (_,_) -> List.of(FluentString.of("true_2"))  );
        assertEquals(
                "true_2",
                FTLTestUtils.fmt(bundle, "named-args")
        );


        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertEquals( 1, options.asInt( "x" ).getAsInt() );
            assertEquals( "Y", options.asString( "y" ).get() );
        } );
        funFactory.setInjected( (_,_) -> List.of(FluentString.of("true_3"))  );
        assertEquals(
                "true_3",
                FTLTestUtils.fmt(bundle, "dense-named-args")
        );

        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertEquals( 1, options.asInt( "x" ).getAsInt() );
            assertEquals( "Y", options.asString( "y" ).get() );
        } );
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 3, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals( 1, rp.positional( 1 ).size() );
            assertEquals( 1, rp.positional( 2 ).size() );

            assertEquals(
                    FluentNumber.of(1),
                    rp.positional( 0 ).getFirst()
            );

            assertEquals(
                    FluentString.of("a"),
                    rp.positional( 1 ).getFirst()
            );

            // not a literal, and 'msg' is unknown
            assertEquals(
                    FluentError.of( "{Unknown message: 'msg'}" ),
                    rp.positional( 2 ).getFirst()
            );

            return List.of( FluentString.of( "true_4" ) );
        } );
        assertEquals(
                "true_4",
                FTLTestUtils.fmt(bundle, "mixed-args")
        );
    }

    @Test
    public void whitespaceAroundArgs1() {
        funAMSGx1();
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "sparse-inline-call")
        );

        funAMSGx1();
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "multiline-call")
        );

        funAMSGx1();
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "sparse-multiline-call")
        );
    }

    @Test
    public void whitespaceAroundArgs2() {
        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertTrue(  options.isEmpty());
        } );
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 0, rp.positionalCount() );
            return List.of(FluentString.of("true_empty"));
        });
        assertEquals(
                "true_empty",
                FTLTestUtils.fmt(bundle, "empty-inline-call")
        );

        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertTrue(  options.isEmpty());
        } );
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 0, rp.positionalCount() );
            return List.of(FluentString.of("true_empty"));
        });
        assertEquals(
                "true_empty",
                FTLTestUtils.fmt(bundle, "empty-multiline-call")
        );
    }

    @Test
    public void whitespaceAroundArgs3Unindented() {
        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals(
                    FluentNumber.of(1L),
                    rp.positional( 0 ).getFirst()
            );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-arg-number")
        );


        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals(
                    FluentString.of("a"),
                    rp.positional( 0 ).getFirst()
            );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-arg-string")
        );


        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals(
                    FluentError.of( "{Unknown message: 'msg'}" ),
                    rp.positional( 0 ).getFirst()
            );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-arg-msg-ref")
        );


        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals(
                    FluentError.of( "{Unknown term: -msg}" ),
                    rp.positional( 0 ).getFirst()
            );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-arg-term-ref")
        );

        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals(
                    FluentError.of( "{Unknown variable: $var}" ),
                    rp.positional( 0 ).getFirst()
            );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-arg-var-ref")
        );

        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals(
                    FluentError.of( "{Unknown function: OTHER()}" ),
                    rp.positional( 0 ).getFirst()
            );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-arg-call")
        );

        funFactory.clear();
        funFactory.setOptionsChecker(  (options) -> {
            assertEquals(
                    1,
                    options.asInt("x").getAsInt()
            );
        } );
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 0, rp.positionalCount() );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-named-arg")
        );

        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals(
                    FluentError.of( "{Unknown message: 'x'}" ),
                    rp.positional( 0 ).getFirst()
            );
            return List.of( FluentString.of( "true!" ) );
        } );
        assertEquals(
                "true!",
                FTLTestUtils.fmt(bundle, "unindented-closing-paren")
        );
    }

        // setup for FUN("a",msg,x:1) in various forms
    private void funAMSGx1() {
        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertEquals( 1, options.asInt( "x" ).getAsInt() );
        } );
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 2, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals( 1, rp.positional( 1 ).size() );

            assertEquals(
                    FluentString.of("a"),
                    rp.positional( 0 ).getFirst()
            );

            // not a literal, and 'msg' is unknown
            assertEquals(
                    FluentError.of( "{Unknown message: 'msg'}" ),
                    rp.positional( 1 ).getFirst()
            );

            return List.of( FluentString.of( "true!" ) );
        } );
    }


    @Test
    public void optionalTrailingComma() {
        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 1, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );

            assertEquals(
                    FluentNumber.of(1),
                    rp.positional( 0 ).getFirst()
            );

            return List.of( FluentString.of( "true_comma" ) );
        } );
        assertEquals(
                "true_comma",
                FTLTestUtils.fmt(bundle, "one-argument")
        );

        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 3, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals( 1, rp.positional( 1 ).size() );
            assertEquals( 1, rp.positional( 2 ).size() );

            assertEquals(
                    FluentNumber.of(1),
                    rp.positional( 0 ).getFirst()
            );

            assertEquals(
                    FluentNumber.of(2),
                    rp.positional( 1 ).getFirst()
            );

            assertEquals(
                    FluentNumber.of(3),
                    rp.positional( 2 ).getFirst()
            );

            return List.of( FluentString.of( "true_comma" ) );
        } );
        assertEquals(
                "true_comma",
                FTLTestUtils.fmt(bundle, "many-arguments")
        );

        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 3, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals( 1, rp.positional( 1 ).size() );
            assertEquals( 1, rp.positional( 2 ).size() );

            assertEquals(
                    FluentNumber.of(1),
                    rp.positional( 0 ).getFirst()
            );

            assertEquals(
                    FluentNumber.of(2),
                    rp.positional( 1 ).getFirst()
            );

            assertEquals(
                    FluentNumber.of(3),
                    rp.positional( 2 ).getFirst()
            );

            return List.of( FluentString.of( "true_comma" ) );
        } );
        assertEquals(
                "true_comma",
                FTLTestUtils.fmt(bundle, "inline-sparse-args")
        );

        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 2, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals( 1, rp.positional( 1 ).size() );

            assertEquals(
                    FluentNumber.of(1),
                    rp.positional( 0 ).getFirst()
            );

            assertEquals(
                    FluentNumber.of(2),
                    rp.positional( 1 ).getFirst()
            );

            return List.of( FluentString.of( "true_comma" ) );
        } );
        assertEquals(
                "true_comma",
                FTLTestUtils.fmt(bundle, "multiline-args")
        );

        funFactory.clear();
        funFactory.setInjected( (rp, scope) -> {
            assertEquals( 2, rp.positionalCount() );
            assertEquals( 1, rp.positional( 0 ).size() );
            assertEquals( 1, rp.positional( 1 ).size() );

            assertEquals(
                    FluentNumber.of(1),
                    rp.positional( 0 ).getFirst()
            );

            assertEquals(
                    FluentNumber.of(2),
                    rp.positional( 1 ).getFirst()
            );

            return List.of( FluentString.of( "true_comma" ) );
        } );
        assertEquals(
                "true_comma",
                FTLTestUtils.fmt(bundle, "multiline-sparse-args")
        );
    }

    @Test
    public void whitespaceNamedArgs() {
        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertEquals( 3, options.size() );
            assertEquals( 1, options.asInt( "x" ).getAsInt() );
            assertEquals( 2, options.asInt( "y" ).getAsInt() );
            assertEquals( 3, options.asInt( "z" ).getAsInt() );
        } );
        funFactory.setInjected( (_,_) -> List.of(FluentString.of("ws_named_1"))  );
        assertEquals(
                "ws_named_1",
                FTLTestUtils.fmt(bundle, "sparse-named-arg")
        );

        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertEquals( 1, options.size() );
            assertEquals( 1, options.asInt( "x" ).getAsInt() );
        } );
        funFactory.setInjected( (_,_) -> List.of(FluentString.of("ws_named_2"))  );
        assertEquals(
                "ws_named_2",
                FTLTestUtils.fmt(bundle, "unindented-colon")
        );

        funFactory.clear();
        funFactory.setOptionsChecker( (options) -> {
            assertEquals( 1, options.size() );
            assertEquals( 1, options.asInt( "x" ).getAsInt() );
        } );
        funFactory.setInjected( (_,_) -> List.of(FluentString.of("ws_named_3"))  );
        assertEquals(
                "ws_named_3",
                FTLTestUtils.fmt(bundle, "unindented-value")
        );
    }




    /// A function, for testing ONLY, that allows us to evaluate
    /// Function arguments. This function must not be cached, and
    /// we will set factory arguments prior to creation so we can
    ///  verify the arguments. This test function IS NOT threadsafe
    @NullMarked
    private static class FUNFactory implements FluentFunctionFactory<FluentFunction.Transform> {

        private FluentFunction.@Nullable Transform inject = null;
        private @Nullable Consumer<Options> checker = null;

        @Override
        public synchronized FluentFunction.Transform create(Locale locale, Options options) {
            if (checker != null) {
                checker.accept( options );
                checker = null;
            }

            if (inject != null) {
                // only use inject once!
                final FluentFunction.Transform transform = inject;
                inject = null;
                return transform;
            } else {
                throw new IllegalStateException( "inject: not set or already used" );
            }
        }

        public synchronized void setInjected(FluentFunction.Transform transform) {
            this.inject = Objects.requireNonNull( transform );
        }

        public synchronized void setOptionsChecker(Consumer<Options> checker) {
            this.checker = Objects.requireNonNull( checker );
        }

        public synchronized void clear() {
            inject = null;
            checker = null;
        }


        @Override
        public boolean canCache() {
            return false;
        }

        @Override
        public String name() {
            return "FUN";
        }
    }


}
