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
import fluent.syntax.ast.Commentary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.Locale;

import static fluent.syntax.parser.FTLParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommentsTest {

    static final String RESOURCE = "fixtures/comments.ftl";
    static FluentResource resource;
    static FluentBundle bundle;



    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );

        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .build();

        bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .build();
    }

    @Test
    public void verifyExceptions() {
        assertEquals( 3, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 18 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 19 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 20 ) );
    }

    @Test
    public void standaloneComment1() {
        resource.entries().stream()
                .filter( Commentary.Comment.class::isInstance )
                .map( Commentary.Comment.class::cast )
                .map( Commentary.Comment::text)
                .filter( "Standalone Comment"::equals )
                .findFirst()
                .orElseThrow( () -> new AssertionError("Mismatch") );
    }

    @Test
    public void standaloneComment2() {
        resource.entries().stream()
                .filter( Commentary.Comment.class::isInstance )
                .map( Commentary.Comment.class::cast )
                .map( Commentary.Comment::text)
                .filter( "Another standalone\n\n     with indent"::equals )
                .findFirst()
                .orElseThrow( () -> new AssertionError("Mismatch") );
    }

    @Test
    public void messageComment() {
        assertEquals(
                "Message Comment",
        bundle.message( "foo" ).orElseThrow()
                .comment().text()
        );
    }

    @Test
    public void termComment() {
        assertEquals(
                "Term Comment\nwith a blank last line.",
                bundle.term( "term" ).orElseThrow()
                        .comment().text()
        );
    }


    @Test
    public void groupComment() {
        resource.entries().stream()
                .filter( Commentary.GroupComment.class::isInstance )
                .map( Commentary.GroupComment.class::cast )
                .map( Commentary.GroupComment::text)
                .filter( "Group Comment"::equals )
                .findFirst()
                .orElseThrow( () -> new AssertionError("Mismatch") );
    }

    @Test
    public void resourceComment() {
        resource.entries().stream()
                .filter( Commentary.ResourceComment.class::isInstance )
                .map( Commentary.ResourceComment.class::cast )
                .map( Commentary.ResourceComment::text)
                .filter( "Resource Comment"::equals )
                .findFirst()
                .orElseThrow( () -> new AssertionError("Mismatch") );
    }

    @Test
    public void errors() {
        // standalone comment for the Errors
        resource.entries().stream()
                .filter( Commentary.Comment.class::isInstance )
                .map( Commentary.Comment.class::cast )
                .map( Commentary.Comment::text)
                .filter( "Errors"::equals )
                .findFirst()
                .orElseThrow( () -> new AssertionError("Mismatch") );
    }




}
