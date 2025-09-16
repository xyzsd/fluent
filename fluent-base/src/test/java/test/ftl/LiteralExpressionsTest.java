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
import fluent.bundle.FluentResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LiteralExpressionsTest {


    static final String RESOURCE = "fixtures/literal_expressions.ftl";
    static FluentResource resource;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );

        // this test file has a duplicate key for the message
        // this will parse OK, but bundle creation will fail
        //
        // This makes the test implementation very annoying as we
        // are not comparing the object model directly
        //
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }

    @Test
    public void verifyEntries() {
        assertEquals( 3, resource.entries().size() );
    }

    @Test
    void stringExpression() {
        final FluentResource r2 = new FluentResource( List.of(resource.entries().getFirst()), List.of(), List.of() );
        final FluentBundle bundle = FTLTestUtils.basicBundleSetup( r2, false );
        assertEquals(
                "abc",
                FTLTestUtils.fmt( bundle, "string-expression" )
        );
    }

    @Test
    void numExpression_1() {
        final FluentResource r2 = new FluentResource( List.of(resource.entries().get(1)), List.of(), List.of() );
        final FluentBundle bundle = FTLTestUtils.basicBundleSetup( r2, false );
        assertEquals(
                "123",
                FTLTestUtils.fmt( bundle, "number-expression" )
        );
    }

    @Test
    void numExpression_2() {
        final FluentResource r2 = new FluentResource( List.of(resource.entries().getLast()), List.of(), List.of() );
        final FluentBundle bundle = FTLTestUtils.basicBundleSetup( r2, false );
        assertEquals(
                "-3.14",
                FTLTestUtils.fmt( bundle, "number-expression" )
        );
    }




}
