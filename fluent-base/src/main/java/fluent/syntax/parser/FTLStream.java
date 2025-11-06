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

import org.jspecify.annotations.NullMarked;

import java.nio.charset.StandardCharsets;

/// Read-only UTF-8 input over FTL source used by the parser.
/// FTLStream wraps a byte array or buffer and provides performant primitives tailored to
/// the Fluent syntax parsing. The underlying data is immutable; a lightweight cursor (position)
/// advances as the parser consumes input.
///
///
@NullMarked
final class FTLStream {

    // implementation
    private final FTLStreamOps ops;

    // UTF8 octets
    private final byte[] seq;

    // current position in seq[]
    private int pos;


    ///  Constructor. No copy.
    FTLStream(final FTLStreamOps ops, final byte[] array) {
        this.ops = ops;
        this.seq = array;
        this.pos = 0;
    }


    /// Calculate the 1-based line number corresponding to the current cursor position.
    ///
    /// If the current position is out-of-bounds (e.g., EOF), 0 is returned.
    ///
    /// @return the line number (>= 0); 0 indicates an invalid position
    int positionToLine() {
        return CommonOps.positionToLine( seq,  pos );
    }

    /// Calculate the 1-based line number for an explicit position in the stream.
    ///
    /// If the position is invalid (e.g., EOF), 0 is returned.
    ///
    /// @param position a byte offset into the decoded stream
    /// @return the line number (>= 0); 0 indicates an invalid position (typically EOF)
    int positionToLine(final int position) {
        return CommonOps.positionToLine( seq, position );
    }

    /// length of the underlying byte array (does not including padding at end, if any)
    int length() {
        return seq.length;
    }

    /// query position
    int position() {
        return pos;
    }

    /// true if we are not EOF
    boolean hasRemaining() {
        return (pos < seq.length);
    }

    /// set position
    void setPosition(int value) {
        pos = value;
    }


    // //////////////////////////////////////////////////////////
    // static utility
    // //////////////////////////////////////////////////////////

    void inc() {
        //assert pos < size;
        pos += 1;
    }

    void inc(final int increment) {
        //assert pos < size;
        pos += increment;
    }

    void dec() {
        //assert pos > 0;
        pos -= 1;
    }

    void dec(final int decrement) {
        //assert pos > decrement;
        pos -= decrement;
    }

    /// Return the byte at the current position. Does NOT change position.
    byte at() {
        // assert (pos >= 0 && pos < size);
        return seq[pos];
    }


    // //////////////////////////////////////////////////////////
    // general methods used by parser (package private)
    // //////////////////////////////////////////////////////////

    /// Return the byte at the specified position, or EOF if out of bounds.
    /// Fail (exception) if negative
    byte at(final int position) {
        // assert (pos >= 0 && pos < size);
        return seq[position];
    }

    String subString(final int startIndex, final int endIndex) {
        // This is a performance-critical section, but entirely dependent on JDK conversion
        // of UTF8 to String performance.
        //
        // An alternative approach could be to keep the CharsetDecoder as a final field
        // (NOT static, CharsetDecoders are not threadsafe):
        //      final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        //      decoder.reset();
        //      decoder.decode( ByteBuffer.wrap( seq, startIndex, (endIndex-startIndex) )).toString();
        //
        return new String( seq, startIndex, (endIndex - startIndex), StandardCharsets.UTF_8 );
    }

    /// this method is not strictly needed
    boolean isCurrentChar(final byte b) {
        return (at() == b);
    }

    /// peek at next byte (relative to current position); return true if matches.
    /// DOES NOT increment position. This does verify that the next character is within bounds.
    boolean isNextChar(final byte b) {
        return ((pos < (seq.length - 1)) && (seq[pos + 1] == b));
    }


    /// throw exception if byte not what expected; otherwise, increment position
    /// This will also throw an exception if we are out of bounds (only positive bound checked)
    void expectChar(final byte b) {
        if (pos >= seq.length || at() != b) {
            throw FTLParser.parseException( FTLParseException.ErrorCode.E0003,
                    CommonOps.byteToString( b ), this );
        }
        pos++;
    }

    /// consume iff match
    boolean takeCharIf(final byte b) {
        if ((pos < seq.length) && at() == b) {
            pos++;
            return true;
        }
        return false;
    }




    int skipBlankBlock() {
        final long packed = ops.skipBlankBlock( seq, pos );
        pos = CommonOps.unpackPosition( packed ); // set position
        return CommonOps.unpackLineCount( packed );
    }


    //   pos = ops.skipBlankBlockNLC( seq,  size, pos );
    void skipBlankBlockNLC() {
        pos = ops.skipBlankBlockNLC( seq, pos );
    }

    void skipBlank() {
        pos = ops.skipBlank( seq, pos );
    }


    int skipBlankInline() {
        final int initialPos = pos;
        pos = ops.skipBlankInline( seq, pos );
        return (pos - initialPos);
    }


    /// skip to the end of line (e.g., for skipping comments).
    /// This will skip to the end of a newline, ignoring a preceding '\r' if present.
    void skipToEOL() {
        pos = ops.skipToEOL( seq, pos );
    }


    /// this does NOT set the position, just finds the position of the end identifier
    /// or returns length()
    int getIdentifierEnd(int startIndex) {
        return ops.getIdentifierEnd( seq, startIndex );
    }





    boolean skipEOL() {
        final byte b = at();
        if (b == '\n') {
            pos++;
            return true;
        } else if (b == '\r' && isNextChar( (byte) '\n' )) {
            pos += 2;
            return true;
        }

        return false;
    }




    boolean isEOL() {
        final byte b = at();
        return ((b == '\n') || (b == '\r' && isNextChar( (byte) '\n' )));
    }


    ///  only used in catch() blocks during parsing, to allow parsing to continue.
    void skipToNextEntryStart() {
        byte prior = (pos > 0) ? seq[pos - 1] : (byte) '\n';
        while (pos < seq.length) {
            final byte b = seq[pos];
            if (prior == '\n') {
                if (CommonOps.isASCIIAlphabetic( b ) || b == '-' || b == '#') {
                    break;
                }
            }
            pos++;
            prior = b;
        }
    }

    // scalar version
    FTLPatternParser.TextSlice getTextSlice() {
        final int startPosition = position();
        FTLPatternParser.TextElementType textElementType = FTLPatternParser.TextElementType.Blank;

        while (hasRemaining()) {
            final byte cb = at();
            if (cb == ' ') {
                inc();
            } else if (cb == '\n') {
                inc();
                return new FTLPatternParser.TextSlice( startPosition, position(),
                        textElementType, FTLPatternParser.TextElementTermination.LineFeed );
            } else if (cb == '\r' && isNextChar( (byte) '\n' )) {
                inc();
                return new FTLPatternParser.TextSlice( startPosition,
                        position() - 1,              // exclude '\r'
                        textElementType,
                        FTLPatternParser.TextElementTermination.CRLF );
            } else if (cb == '{') {
                return new FTLPatternParser.TextSlice( startPosition, position(),
                        textElementType, FTLPatternParser.TextElementTermination.PlaceableStart );
            } else if (cb == '}') {
                throw FTLParser.parseException( FTLParseException.ErrorCode.E0027, this );
            } else {
                inc();
                textElementType = FTLPatternParser.TextElementType.NonBlank;
            }
        }

        return new FTLPatternParser.TextSlice( startPosition, position(),
                textElementType, FTLPatternParser.TextElementTermination.EOF );
    }


    /// Parses a 4 or 6 byte hex sequence into a valid Unicode code point.
    ///
    /// *NOTE:* this method returns a Unicode code point (as an int)
    ///
    /// @param requiredLength 4 or 6 hex bytes
    /// @return code point as an int (which is equivalent to a Java String of 1 or 2 characters)
    /// @throws FTLParseException if hex sequence is not of the required length, or is not a valid code point.
    int getUnicodeEscape(final int requiredLength) {
        final int start = position();
        int codePoint = 0;      // calculate in situ
        int count = 0;
        while ((count < requiredLength) && hasRemaining()) {
            final byte b = at();
            if (CommonOps.isASCIIHex( b )) {
                inc();
                count++;
                codePoint *= CommonOps.RADIX;
                codePoint += Character.digit( b, CommonOps.RADIX );
            } else {
                break;
            }
        }

        if (((position() - start) != requiredLength) || !Character.isValidCodePoint( codePoint )) {
            final int end = (position() > requiredLength) ? position() : (position() + 1);
            throw FTLParseException.of(
                    FTLParseException.ErrorCode.E0026,
                    subString( start, end ),
                    positionToLine()
            );
        }

        return codePoint;
    }

    boolean isIdentifierStart() {
        return CommonOps.isASCIIAlphabetic( at() );
    }

    boolean isBytePatternContinuation() {
        return !CommonOps.isLineStart( at() );
    }

    boolean isNumberStart() {
        final byte b = at();
        return (b == '-' || CommonOps.isASCIIDigit( b ));
    }

    void skipDigits() {
        final int start = position();
        // no need for explicit EOF check, isASCIIDigit(EOF) == false
        while (CommonOps.isASCIIDigit( at() )) {
            inc();
        }
        if (start == position()) {
            throw FTLParser.parseException( FTLParseException.ErrorCode.E0004,
                    "0-9",
                    this
            );
        }
    }


    /// For debugging: return character at given position
    String dbg(final int position) {
        return "offset: " + position + ": " + CommonOps.byteToString( at( position ) );
    }

    /// For debugging: return character at current position
    @SuppressWarnings("unused")
    String dbg() {
        return dbg( position() );
    }

    ///  For debugging: display the bytes from start (inclusive) to end (exclusive)
    @SuppressWarnings("unused")
    String dbg(int start, int end) {
        StringBuilder sb = new StringBuilder( end - start );
        for (int i = start; i < end; i++) {
            sb.append( CommonOps.byteToString( seq[i] ) );
            sb.append( ' ' );
        }
        return sb.toString();
    }

}
