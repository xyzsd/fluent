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

package test.ftl;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import fluent.syntax.AST.Message;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class crMultlineValueTest {

    static final String RESOURCE = "fixtures/cr_multilinevalue.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup(resource, false);
    }



    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    @Test
    public void verifyEntries() {
        assertEquals( 1, resource.entries().size() );
    }

    @Test
    public void verifyNoAttribute() {
        // there should be no attribute (because of CR as EOL)
        final Message key01 = bundle.message( "key01" ).orElseThrow();
        assertEquals( 0, key01.attributes().size() );
    }


    @Test
    public void testKey01() {
        assertEquals(
                "\r\r    Value 03\r    Continued\r\r    and continued\r    \r    and continued\r\r    .title = Title\r\r\r### This entire file uses CR as EOL.\r",
                FTLTestUtils.fmt( bundle, "key01" )
        );
    }



}