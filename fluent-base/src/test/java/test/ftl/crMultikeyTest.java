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
import fluent.syntax.AST.Entry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class crMultikeyTest {

    static final String RESOURCE = "fixtures/cr_multikey.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup(resource, false);

        resource.entries().forEach( entry -> {System.out.println(entry);});
        final Entry first = resource.entries().getFirst();
        /*
        if (first instanceof Message msg) {
            System.out.println(msg.pattern().elements().size());
            final PatternElement p = msg.pattern().elements().getFirst();
            if(p instanceof PatternElement.TextElement t) {
                for(int i=0; i<t.value().length(); i++) {
                    System.out.println(t.value().charAt(i) + " : "+Integer.toHexString( (int)t.value().charAt(i)));
                }
            }
            msg.pattern().elements().forEach( entry -> {System.out.println(entry);});
            msg.pattern().elements().forEach( pattern -> { System.out.println("|"+pattern+"|");});
        }
        */
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
    public void testKey01() {
        assertEquals(
                "Value 01\rerr02 = Value 02\r\r\r### This entire file uses CR as EOL.\r",
                FTLTestUtils.fmt( bundle, "key01" )
        );
    }


}