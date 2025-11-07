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

package test.misc;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

///  Miscellaneous FTL tests. Early-EOF tests for parser correctness, some of which are not included in FTL test fixtures.
///  We are just testing the parser here; we are not creating a bundle.
public class FmtBuilderTest {

    // TODO: capture error logs and test them as well

    static final String RESOURCE = "misc/fmt_builder.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, true );
    }


    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }


    @Test
    public void messageOne() {
        String renderedMessage = bundle.fmtBuilder("messageOne")
                        .format();

        assertEquals(
                "Hello, World!",
                renderedMessage
        );
    }

    @Test
    public void messageTwo() {
        String renderedMessage = bundle.fmtBuilder("messageTwo")
                .arg("person", "Mr. F. Luent")
                .format();

        assertEquals(
                "Hello there, Mr. F. Luent, how are you today?",
                renderedMessage
        );
    }


    @Test
    public void confirmMessage() {
        final String baseMessage = bundle.fmtBuilder("confirmMessage")
                .format();

        // base message has no pattern
        assertEquals(
                "{No pattern specified for message: 'confirmMessage'}",
                baseMessage
        );

        final String attrib1 = bundle.fmtBuilder("confirmMessage")
                .attribute( "ok" )
                .format();

        // but attributes do
        assertEquals(
                "OK!",
                attrib1
        );


        final String attrib2 = bundle.fmtBuilder("confirmMessage")
                .attribute( "cancel" )
                .format();

        assertEquals(
                "Cancel!",
                attrib2
        );
    }


    @Test
    public void fallback_1() {

        final String msg_key_missing = bundle.fmtBuilder("this-key-does-not-exist")
                .formatOrElse( "FALLBACK MESSAGE no key" );

        assertEquals(
                "FALLBACK MESSAGE no key",
                msg_key_missing
        );

        final String attribute_missing = bundle.fmtBuilder("confirmMessage")
                .attribute( "this-attribute-does-not-exist" )
                .formatOrElse( "FALLBACK MESSAGE no attribute" );

        assertEquals(
                "FALLBACK MESSAGE no attribute",
                attribute_missing
        );

        final String msg_key_missing_with_supplier = bundle.fmtBuilder("this-key-does-not-exist")
                .formatOrElseGet( () -> "supplied FALLBACK MESSAGE no key" );

        assertEquals(
                "supplied FALLBACK MESSAGE no key",
                msg_key_missing_with_supplier
        );
    }

    @Test
    public void fallback_2() {
       // missing variable
        String renderedMessage = bundle.fmtBuilder("messageTwo")
                .arg("a-very-severe-typo", "Mr. F. Luent")
                .formatOrElse( "FALLBACK MESSAGE" );

        assertEquals(
                "FALLBACK MESSAGE",
                renderedMessage
        );


    }

    @Test
    public void fallback_3() {
        assertThrows( IllegalArgumentException.class,  () -> {
            String renderedMessage = bundle.fmtBuilder("messageTwo")
                    .arg("a-very-severe-typo", "Mr. F. Luent")
                    .formatOrThrow( () -> new IllegalArgumentException("Illegal Message") );
        });
    }

    @Test
    public void supplierFailure() {

        final Supplier<String> failingSupplier = () -> { throw new IllegalStateException(); };

        final String msg = bundle.fmtBuilder("this-key-does-not-exist-failing-supplier-test")
                .formatOrElseGet( failingSupplier );

        assertEquals(
                "{Message 'this-key-does-not-exist-failing-supplier-test' fallback failure!}",
                msg
        );
    }

}

