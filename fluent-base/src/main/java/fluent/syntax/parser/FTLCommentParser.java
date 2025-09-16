/*
 *
 *  Copyright (C) 2021-2025, xyzsd (Zach Del) 
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
 */

package fluent.syntax.parser;

import fluent.syntax.AST.Commentary;
import org.jspecify.annotations.NullMarked;

/// This breaks out Comment parsing from the main parser, to improve code clarity
@NullMarked
class FTLCommentParser {

    static Commentary getComment(FTLStream ps) {
        int level = -1;
        final StringBuilder sb = new StringBuilder( 64 );

        while (ps.hasRemaining()) {
            final int lineLevel = getCommentLevel( ps );
            if (lineLevel == 0) {
                ps.dec();
                break;
            }

            if (level != -1 && (level != lineLevel)) {
                ps.dec( lineLevel );
                break;
            }

            level = lineLevel;

            final byte cb = ps.at();    // cb == current byte
            if (ps.position() == ps.length()) {
                break;
            } else if (ps.isEOL()) {
                // handles LF *and* CRLF
                sb.append( getCommentLine( ps ) );
            } else {
                if (cb == ' ') {
                    ps.inc();
                } else {
                    if (sb.isEmpty()) {
                        throw FTLParser.parseException( ParseException.ErrorCode.E0003,
                                " ", ps );
                    } else {
                        ps.dec( lineLevel );
                        break;
                    }
                }
                String s = getCommentLine( ps );
                if (!sb.isEmpty()) {
                    sb.append( '\n' );
                }
                sb.append( s );
            }
            ps.skipEOL();
        }

        return switch (level) {
            case 3 -> new Commentary.ResourceComment( sb.toString() );
            case 2 -> new Commentary.GroupComment( sb.toString() );
            default -> new Commentary.Comment( sb.toString() );
        };
    }

    private static int getCommentLevel(FTLStream ps) {
        int nChars = 0;
        while (ps.takeCharIf( (byte) '#' )) {
            nChars += 1;
        }
        return nChars;
    }

    private static String getCommentLine(FTLStream ps) {
        final int startPos = ps.position();
        while (ps.hasRemaining() && !ps.isEOL()) {
            ps.inc();
        }
        return ps.subString( startPos, ps.position() );
    }



    private FTLCommentParser() {}
}
