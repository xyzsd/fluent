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

package ftl;

import fluent.bundle.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectExpressionsTest {

    static final String RESOURCE = "fixtures/select_expressions.ftl";
    static FluentResource resource;
    static FluentBundle bundle;
    static FTLTestUtils.FnFactory fnFactoryBUILTIN;
    static FTLTestUtils.FnFactory fnFactoryFOO;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
                requireNonNull( resource, "FluentResource cannot be null" );


        fnFactoryBUILTIN = new FTLTestUtils.FnFactory("BUILTIN");
        fnFactoryFOO = new FTLTestUtils.FnFactory("FOO");

        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .addFactory( fnFactoryBUILTIN )
                .addFactory( fnFactoryFOO )
                .build();


       bundle = FluentBundle.builder( Locale.US, registry, FluentFunctionCache.uncached() )
                .addResource( resource )
               .build();
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 6, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0017, 14 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0017, 20 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0029, 26 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0029, 34 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 58 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0027, 64 ) );
    }

    @Test
    public void newMessages() {
        fnFactoryBUILTIN.reset();
        fnFactoryBUILTIN.setTransform(  (_,_) -> {} );
        fnFactoryBUILTIN.setResult("0");
        assertEquals(
                "Zero",
                FTLTestUtils.fmt( bundle, "new-messages" )
        );

        fnFactoryBUILTIN.reset();
        fnFactoryBUILTIN.setTransform(  (_,_) -> {} );
        fnFactoryBUILTIN.setResult("other");
        assertEquals(
                "Other",
                FTLTestUtils.fmt( bundle, "new-messages" )
        );
    }

    @Test
    public void validSelectorTermAttribute() {
        assertEquals(
                "value",
                FTLTestUtils.fmt( bundle, "valid-selector-term-attribute" )
        );
    }

    @Test
    public void emptyVariant() {
        assertEquals(
                "",
                FTLTestUtils.fmt( bundle, "empty-variant" )
        );

        assertEquals(
                "",
                FTLTestUtils.fmt( bundle, "empty-variant", Map.of("sel","blah") )
        );
    }

    @Test
    public void reducedWhitespace() {
        fnFactoryFOO.reset();
        fnFactoryFOO.setTransform(  (_,_) -> {} );
        fnFactoryFOO.setResult("doesn't-matter");
        assertEquals(
                "",
                FTLTestUtils.fmt( bundle, "reduced-whitespace" )
        );
    }

    @Test
    public void nestedSelect() {
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "nested-select" )
        );

        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "nested-select",Map.of("sel","blah") )
        );
    }



}