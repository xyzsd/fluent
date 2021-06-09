/*
 *
 *  Copyright (C) 2021, xyzsd (Zach Del)
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
 */

package fluent.syntax.parser;


import fluent.syntax.AST.Expression;
import fluent.syntax.AST.Pattern;
import fluent.syntax.AST.PatternElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This breaks out Pattern parsing from the main parser, to improve code clarity
 */
class FTLPatternParser {


    // This enum tracks the reason for which a text slice ended.
    // It is used by the pattern to set the proper state for the next line.
    //
    // CRLF variant is specific because we want to skip the CR but keep the LF in TextElements
    // For example `a\r\n b` will produce (`a`, `\n` and ` b`) TextElements.
    private enum TextElementTermination {
        LineFeed,
        CRLF,
        PlaceableStart,
        EOF,
    }


    // This enum tracks the placement of the text element in the pattern, which is needed for
    // deindentation logic.
    enum TextElementPosition {
        InitialLineStart,
        LineStart,
        Continuation,
    }


    // This enum tracks whether the text element is blank or not.
    // This is important to identify text elements which should not be taken into account
    // when calculating common indent.
    private enum TextElementType {
        Blank,
        NonBlank,
    }

    // A slice of text
    private static record TextSlice(int start,
                                    int end,
                                    TextElementType textElementType,
                                    TextElementTermination terminationReason) {}


    // parse a pattern
    // NOTE: Profiling indicates that method dominates parsing time (expectedly)
    static Optional<Pattern> getPattern(FTLStream ps) {
        List<PEPlaceholder> elements = new ArrayList<>(4);
        int lastNonBlank = -1;
        int commonIndent = -1;
        ps.skipBlankInline();

        TextElementPosition textElementRole;
        if (ps.skipEOL()) {
            ps.skipBlankBlock();
            textElementRole = TextElementPosition.LineStart;
        } else {
            textElementRole = TextElementPosition.InitialLineStart;
        }

        while (ps.hasRemaining()) {
            if (ps.isCurrentChar( '{' )) {
                if (textElementRole == TextElementPosition.LineStart) {
                    commonIndent = 0;
                }
                final Expression exp = FTLParser.getPlaceable( ps );
                lastNonBlank = elements.size();
                elements.add( PEPlaceholder.of( new PatternElement.Placeable(exp) ) );
                textElementRole = TextElementPosition.Continuation;
            } else {
                final int sliceStart = ps.position();
                int indent = 0;
                if (textElementRole == TextElementPosition.LineStart) {
                    indent = ps.skipBlankInline();

                    // fixed with isEOL() to take into account blank lines in UNIX or Windows style
                    if (!ps.hasRemaining() || (indent == 0 && !ps.isEOL())) {
                        break;
                    } else if (!ps.isBytePatternContinuation()) {
                        ps.position( sliceStart );    // rewind
                        break;
                    }
                    // else : continue
                }

                final TextSlice textSlice = getTextSlice( ps );
                if (textSlice.start() != textSlice.end()) {     // == for <CR> alone (ignored)
                    if (textElementRole == TextElementPosition.LineStart
                            && textSlice.textElementType() == TextElementType.NonBlank) {
                        assert (indent >= 0);
                        if (commonIndent != -1) {
                            if (indent < commonIndent) {
                                commonIndent = indent;
                            }
                        } else {
                            commonIndent = indent;
                        }
                        assert (commonIndent >= 0);
                    }

                    if (textElementRole != TextElementPosition.LineStart
                            || textSlice.textElementType() == TextElementType.NonBlank
                            || textSlice.terminationReason() == TextElementTermination.LineFeed) {

                        if (textSlice.textElementType() == TextElementType.NonBlank) {
                            lastNonBlank = elements.size();
                        }

                        elements.add( PEPlaceholder.of(
                                sliceStart,
                                textSlice.end(),
                                indent,
                                textElementRole
                        ) );
                    }
                }

                switch (textSlice.terminationReason()) {
                    case LineFeed -> textElementRole = TextElementPosition.LineStart;
                    case CRLF, PlaceableStart, EOF -> textElementRole = TextElementPosition.Continuation;
                }
            }
        }

        if (lastNonBlank != -1) {
            elements = elements.subList( 0, lastNonBlank + 1 );
            List<PatternElement> patternElements = new ArrayList<>( elements.size() );

            int count = 0;
            for (final PEPlaceholder placeholder : elements) {
                if (placeholder instanceof PEPlaceholder.PlaceableHolder plHolder) {
                    patternElements.add( plHolder.placeable() );
                } else if (placeholder instanceof PEPlaceholder.TextElementHolder textElement) {
                    int start = textElement.start();
                    if(textElement.role() == TextElementPosition.LineStart) {
                        start = (commonIndent == -1)
                                ? (textElement.start() + textElement.indent())
                                : (textElement.start() + Math.min( textElement.indent(), commonIndent ));
                    }

                    String text = ps.subString( start, textElement.end() );

                    if (lastNonBlank == count) {
                        text = text.stripTrailing();
                    }

                    patternElements.add( new PatternElement.TextElement( text ) );

                } else {
                    throw new IllegalStateException();
                }

                count++;
            }

            assert (patternElements.size() == elements.size());
            return Optional.of( new Pattern( patternElements ) );
        }

        return Optional.empty();
    }


    private static TextSlice getTextSlice(FTLStream ps) {
        final int startPosition = ps.position();
        TextElementType textElementType = TextElementType.Blank;

        while (ps.hasRemaining()) {
            final char cb = ps.at();
            if (cb == ' ') {
                ps.inc();
            } else if (cb == '\n') {
                ps.inc();
                return new TextSlice( startPosition, ps.position(),
                        textElementType, TextElementTermination.LineFeed );
            } else if (cb == '\r' && ps.isNextChar( '\n' )) {
                ps.inc();        // TODO: confirm this shouldn't this be +2?
                return new TextSlice( startPosition,
                        ps.position() - 1,              // exclude '\r'
                        textElementType,
                        TextElementTermination.CRLF );
            } else if (cb == '{') {
                return new TextSlice( startPosition, ps.position(),
                        textElementType, TextElementTermination.PlaceableStart );
            } else if (cb == '}') {
                throw ParseException.create( ParseException.ErrorCode.E0027, ps );
            } else {
                ps.inc();
                textElementType = TextElementType.NonBlank;
            }
        }

        return new TextSlice( startPosition, ps.position(),
                textElementType, TextElementTermination.EOF );
    }


    private FTLPatternParser() {}

}
