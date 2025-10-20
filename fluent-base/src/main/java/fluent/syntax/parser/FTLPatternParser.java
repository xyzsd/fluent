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


import fluent.syntax.AST.Expression;
import fluent.syntax.AST.Pattern;
import fluent.syntax.AST.PatternElement;
import fluent.syntax.parser.PEPlaceholder.PlaceableHolder;
import fluent.syntax.parser.PEPlaceholder.TextElementHolder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;


/// This breaks out Pattern parsing from the main parser, to improve code clarity
@NullMarked
final class FTLPatternParser {

    // method handle for getTextSlice(), so we don't have to put it in Accel or FTLStream.
    private static final MethodHandle MH_GTS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType typeOfTarget = MethodType.methodType( TextSlice.class, FTLStream.class );
        try {
            if (FTLStream.isSIMD()) {
                MH_GTS = lookup.findStatic( FTLPatternParser.class, "getTextSliceSIMD", typeOfTarget );
            } else {
                MH_GTS = lookup.findStatic( FTLPatternParser.class, "getTextSliceScalar", typeOfTarget );
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException( e );
        }
    }

    private FTLPatternParser() {}

    // parse a pattern
    // NOTE: Profiling indicates that method dominates parsing time (expectedly)
    static @Nullable Pattern getPattern(final FTLStream ps) {
        List<PEPlaceholder> elements = new ArrayList<>( 4 );
        int lastNonBlank = -1;
        int commonIndent = -1;
        ps.skipBlankInline();

        TextElementPosition textElementRole;
        if (!ps.hasRemaining()) {
            return null;    // EOF
        } else if (ps.skipEOL()) {
            ps.skipBlankBlockNLC();
            textElementRole = TextElementPosition.LineStart;
        } else {
            textElementRole = TextElementPosition.InitialLineStart;
        }

        while (ps.hasRemaining()) {
            if (ps.isCurrentChar( (byte) '{' )) {
                if (textElementRole == TextElementPosition.LineStart) {
                    commonIndent = 0;
                }
                final Expression exp = FTLParser.getPlaceable( ps );
                lastNonBlank = elements.size();
                elements.add( PEPlaceholder.of( new PatternElement.Placeable( exp ) ) );
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
                // since there is no 'next' iteration
                textElementRole = switch (textSlice.terminationReason()) {
                    case LineFeed, CRLF -> TextElementPosition.LineStart;
                    case PlaceableStart, EOF -> TextElementPosition.Continuation;
                    case ERROR -> throw new IllegalStateException();
                };
            }
        }

        if (lastNonBlank != -1) {
            elements = elements.subList( 0, lastNonBlank + 1 );
            List<PatternElement> patternElements = new ArrayList<>( elements.size() );

            int count = 0;
            for (final PEPlaceholder placeholder : elements) {
                switch (placeholder) {
                    case PlaceableHolder(PatternElement.Placeable placeable) -> patternElements.add( placeable );
                    case TextElementHolder(int start, int end, int indent, TextElementPosition role) -> {

                        if (role == TextElementPosition.LineStart) {
                            start = (commonIndent == -1)
                                    ? (start + indent)
                                    : (start + Math.min( indent, commonIndent ));
                        }

                        //  Strip whitespace here if needed.
                        //  We do this by adjusting the end position, since Fluent legal whitespace
                        //  only consists of ASCII characters. With an additional caveat (see below).
                        //  This will work for valid UTF8, and if it is malformed, it will remain malformed.
                        final String text;
                        if (lastNonBlank == count) {
                            // Fluent whitespace is: ASCII space (0x20), LF (0x0a) and CR-LF paired. (CR == 0x0d)
                            // CRs that are NOT paired are not considered whitespace!
                            // We must only remove CR then, if (going from the end->start) an LF precedes.
                            // NOTE: this could be improved with SIMD/SWAR, potentially
                            // NOTE: we use a boolean flag to reduce potential for malicious/invalid Unicode
                            int endIndex = end - 1;       // 'end' is exclusive, 'endIndex' is inclusive
                            boolean priorIsNewline = false;
                            while (start < endIndex) {
                                final byte b = ps.at( endIndex );
                                if (b == ' ' || (priorIsNewline && b == '\r')) {
                                    endIndex--;
                                    priorIsNewline = false;
                                } else if (b == '\n') {
                                    endIndex--;
                                    priorIsNewline = true;
                                } else {
                                    break;
                                }
                            }
                            text = ps.subString( start, endIndex + 1 );
                        } else {
                            text = ps.subString( start, end );
                        }

                        patternElements.add( new PatternElement.TextElement( text ) );
                    }
                }
                count++;
            }

            assert (patternElements.size() == elements.size());
            return new Pattern( patternElements );
        }

        return null;
    }

    // a not-insignificant amount of time is spent in this method based on profiling
    private static TextSlice getTextSlice(FTLStream ps) {
        // The goal here is to use a MethodHandle to encourage inlining by the JVM
        try {
            return (TextSlice) MH_GTS.invokeExact( ps );
        } catch (ParseException p) {
            throw p;
        } catch (Throwable t) {
            throw ParseException.of( t );
        }
    }

    @SuppressWarnings( "unused" )   // used by getTextSLice() via method handle
    private static TextSlice getTextSliceScalar(FTLStream ps) {
        final int startPosition = ps.position();
        TextElementType textElementType = TextElementType.Blank;

        while (ps.hasRemaining()) {
            final byte cb = ps.at();
            if (cb == ' ') {
                ps.inc();
            } else if (cb == '\n') {
                ps.inc();
                return new TextSlice( startPosition, ps.position(),
                        textElementType, TextElementTermination.LineFeed );
            } else if (cb == '\r' && ps.isNextChar( (byte) '\n' )) {
                ps.inc();
                return new TextSlice( startPosition,
                        ps.position() - 1,              // exclude '\r'
                        textElementType,
                        TextElementTermination.CRLF );
            } else if (cb == '{') {
                return new TextSlice( startPosition, ps.position(),
                        textElementType, TextElementTermination.PlaceableStart );
            } else if (cb == '}') {
                throw FTLParser.parseException( ParseException.ErrorCode.E0027, ps );
            } else {
                ps.inc();
                textElementType = TextElementType.NonBlank;
            }
        }

        return new TextSlice( startPosition, ps.position(),
                textElementType, TextElementTermination.EOF );
    }

    @SuppressWarnings( "unused" )   // used by getTextSLice() via method handle
    private static TextSlice getTextSliceSIMD(FTLStream ps) {
        final int startPos = ps.position();
        final long packed = ps.nextTSChar( startPos );
        final TextElementTermination termination = TextElementTermination.VALUES[FTLStream.ordinal( packed )];
        final int endPos = FTLStream.position( packed );

        final TextElementType textElementType = ps.isBlank( startPos, endPos )
                ? TextElementType.Blank
                : TextElementType.NonBlank;

        return switch (termination) {
            case LineFeed -> {
                ps.position( endPos + 1 );
                yield new TextSlice( startPos, endPos + 1,
                        textElementType, termination );
            }
            case CRLF -> {
                ps.position( endPos + 1 );
                yield new TextSlice( startPos, endPos,
                        textElementType, termination );
            }
            case PlaceableStart, EOF -> {
                ps.position( endPos );
                yield new TextSlice( startPos, endPos,
                        textElementType, termination );
            }
            case ERROR -> {
                // unbalanced closing brace
                ps.position( endPos );
                throw FTLParser.parseException( ParseException.ErrorCode.E0027, ps );
            }
        };
    }


    // This enum tracks the reason for which a text slice ended.
    // It is used by the pattern to set the proper state for the next line.
    //
    // CRLF variant is specific because we want to skip the CR but keep the LF in TextElements
    // For example `a\r\n b` will produce (`a`, `\n` and ` b`) TextElements.
    enum TextElementTermination {
        // NOTE: order is critical
        EOF,                // index 0
        LineFeed,           // index 1
        CRLF,               // index 2
        PlaceableStart,     // index 3 '{' open brace
        // ERROR is only used by getTextSlice(). Should not occur anywhere else.
        ERROR;              // index 4 '}' closed brace;

        static final TextElementTermination[] VALUES = TextElementTermination.values();
    }


    // This enum tracks the placement of the text element in the pattern, which is needed for
    // dedentation logic.
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
    record TextSlice(int start,
                     int end,
                     TextElementType textElementType,
                     TextElementTermination terminationReason) {
    }

}
