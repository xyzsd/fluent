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

package ftl;import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import fluent.syntax.AST.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReferenceExpressionsTest {

    static final String RESOURCE = "fixtures/reference_expressions.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );

        resource.entries().forEach( entry -> {System.out.println(entry);});
        resource.errors().forEach( entry -> {System.out.println(entry);});
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 3, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0016, 19 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0017, 23 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0016, 28 ) );
    }

    @Test
    public void msgRefPlaceable() {
        final PatternElement.Placeable placeable = getPlaceable( bundle, "message-reference-placeable" );
        assertTrue( placeable.expression() instanceof InlineExpression.MessageReference );
        final String name = ((InlineExpression.MessageReference) placeable.expression()).name();
        assertEquals(
                "msg",
                name
        );
    }


    @Test
    public void termRefPlaceable() {
        final PatternElement.Placeable placeable = getPlaceable( bundle, "term-reference-placeable" );
        assertTrue( placeable.expression() instanceof InlineExpression.TermReference );
        final String name = ((InlineExpression.TermReference) placeable.expression()).name();
        assertEquals(
                "term",
                name
        );
    }


    @Test
    public void varRefPlaceable() {
        // just test for realz, assigning a value to '$var'
        assertEquals(
                "VARIABLE",
                FTLTestUtils.fmt( bundle, "variable-reference-placeable", Map.of("var","VARIABLE") )
        );
    }

    @Test
    public void fnRefPlaceable() {
        // not really a function reference, just a message key in all caps
        final PatternElement.Placeable placeable = getPlaceable( bundle, "function-reference-placeable" );
        assertTrue( placeable.expression() instanceof InlineExpression.MessageReference );
        final String name = ((InlineExpression.MessageReference) placeable.expression()).name();
        assertEquals(
                "FUN",
                name
        );
    }

    @Test
    public void varRefSelector() {
        // another test for real
        assertEquals(
                "Value",
                FTLTestUtils.fmt( bundle, "variable-reference-selector", Map.of("var","doesn't-matter") )
        );
    }



    // get the 1st placeable found for a given message, but we only go 1 level deep
    private static PatternElement.Placeable getPlaceable(final FluentBundle bndl, final String msgID) {
        assertNotNull( bndl );
        assertNotNull( msgID );

        final Message message = bndl.message( msgID ).orElseThrow( () -> new IllegalArgumentException( "message(" + msgID + ") not found." ) );
        if(message.pattern() == null) {
            throw new  IllegalArgumentException( "message(" + msgID + "): no pattern." );
        }

        for( PatternElement element : message.pattern().elements()) {
            if(element instanceof PatternElement.Placeable p) {
                return p;
            }
        }

        throw new IllegalStateException("No placeable found for message '"+msgID+"'");
    }

}