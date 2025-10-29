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

import fluent.bundle.FluentResource;
import fluent.syntax.parser.FTLParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Test skipBlankBlock()
///
public class SkipBlankBlockTest {




    @Test
    public void attributeEarlyEOF_noEquals() {
        // using regular strings to make construction of this odd input very clear.
        final String in =   "# many lines have blank spaces.\n\n\n\n"+
                            "key01 = value01\n\n\n\n              \n"+
                            "\n                                   \n"+  // many spaces between first and last \n
                            "\n                                   \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "key02 = value02\n"+
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n"+  // many spaces between first and last \n
                            "\n\n              \r\n\r\n                 \n";  // many spaces between first and last \n

        final FluentResource resource = FTLParser.parse( in, FTLParser.ParseOptions.DEFAULT,
                FTLParser.Implementation.AUTO );

        // no errors
        assertEquals( 0, resource.errors().size() );
        // two entries (comment line ignored by parser (ignoreCommentsAndJunk is true)) but if NOT ignored, there would be 3 entries.
        assertEquals( 2, resource.entries().size() );
    }


}