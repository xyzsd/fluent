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

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.FTLTestUtils;

import java.io.IOException;

import static fluent.syntax.parser.ParseException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

///  Astral Plane Unicode Test
/// 'Astral' Unicode planes is the informal name for supplementary planes
public class AstralTest {

    static final String RESOURCE = "fixtures/astral.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }


    @Test
    public void verifyExceptions() {
        // some exceptions can occur during parsing, depending upon the input;
        // verify any exceptions that were raised are indeed present.

        assertEquals( 3, resource.errors().size() );
        assertTrue( FTLTestUtils.matchParseException( resource, E0003, 12 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0028, 15 ) );
        assertTrue( FTLTestUtils.matchParseException( resource, E0004, 19 ) );
    }


    @Test
    public void faceJoy() {
        assertEquals(
                "😂", // aka "\uD83D\uDE02",
                FTLTestUtils.fmt( bundle, "face-with-tears-of-joy" )
        );
    }

    @Test
    public void tetragram() {
        assertEquals(
                "𝌆",
                FTLTestUtils.fmt( bundle, "tetragram-for-centre" )
        );
    }

    @Test
    public void surrogatesInText() {
        assertEquals(
                "\\uD83D\\uDE02",
                FTLTestUtils.fmt( bundle, "surrogates-in-text" )
        );
    }

    @Test
    public void surrogatesInString() {
        assertEquals(
                "\uD83D\uDE02",
                FTLTestUtils.fmt( bundle, "surrogates-in-string" )
        );
    }


    @Test
    public void surrogatesInAdjacentStrings() {
        assertEquals(
                "\uD83D\uDE02",
                FTLTestUtils.fmt( bundle, "surrogates-in-adjacent-strings" )
        );
    }

    @Test
    public void emojiInText() {
        assertEquals(
                "A face \uD83D\uDE02 with tears of joy.",
                FTLTestUtils.fmt( bundle, "emoji-in-text" )
        );
    }

    @Test
    public void emojiInString() {
        assertEquals(
                "A face \uD83D\uDE02 with tears of joy.",
                FTLTestUtils.fmt( bundle, "emoji-in-string" )
        );
    }


}
