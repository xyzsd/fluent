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
/// Implementation note: some implementations may copy the byte buffer prior to use.
///
@NullMarked
sealed class FTLStream permits FTLStreamSWAR, FTLStreamSIMD {

    // EOF (0xFF) is not a valid Unicode character, and should
    // not be present in a stream unless malformed.
    static final byte EOF = (byte) 0xFF;


    // magic constants for scalar ASCII comparisons.
    protected static final byte MAGIC_CAPS_ALPHA_OFFSET = ((byte) (-65 + Byte.MIN_VALUE)); // 'A' (uppercase A)
    protected static final byte MAGIC_LC_ALPHA_OFFSET = ((byte) (-97 + Byte.MIN_VALUE)); // 'a' (lowercase A)
    protected static final byte MAGIC_ALPHA_RANGE = ((byte) (26 + Byte.MIN_VALUE));  // 26 letters

    protected static final byte MAGIC_HEX_LETTER_RANGE = ((byte) (6 + Byte.MIN_VALUE));  // 6 letter range
    protected static final byte MAGIC_DIGIT_RANGE = ((byte) (10 + Byte.MIN_VALUE));    // 10 digit range
    protected static final byte MAGIC_DIGIT_OFFSET = ((byte) (-48 + Byte.MIN_VALUE));    // '0' ascii value

    // internal
    protected static final int RADIX = 16;

    // invariants
    protected final byte[] seq;   // UTF8 octets. seq.length == (size + ACCEL.pad())
    protected final int size;     // size of actual data in seq[]

    // state
    protected int pos;    // current position in stream


    // scalar version. no padding
    FTLStream(final byte[] array) {
        this(array, array.length);
    }

    // for subclasses ONLY
    protected FTLStream(final byte[] array, final int size) {
        this.size = size;
        this.seq = array;
        this.pos = 0;
    }



    ///  True if ASCII ('}','.','[', or '*').
    static boolean isLineStart(final byte b) {
        return (b == '}' || b == '.' || b == '[' || b == '*');
    }

    ///  True if ASCII ('0-9').
    static boolean isASCIIDigit(final byte in) {
        // OLD: return (b >= 48 && b <= 57);  // 0-9
        // all (one...) casts are required!
        return ((byte) (in + MAGIC_DIGIT_OFFSET) < MAGIC_DIGIT_RANGE);
    }


    // //////////////////////////////////////////////////////////
    // simple package-private methods
    // //////////////////////////////////////////////////////////

    ///  True if ASCII ('a-z','A-Z').
    static boolean isASCIIAlphabetic(final byte in) {
        // Simple approach (with many comparisons):
        //      return ((in >= 97 && in <= 122) || (in >= 65 && in  <= 90)); // a-z || A-Z
        //
        // Fancy approach:
        // (1) byte b1 = in | 0x20; // convert uppercase to lowercase ASCII by flipping this bit
        //      now we only have 1 range to check (lowercase 'a .. z')
        //
        // (2) byte b2 = (b1 - 97); subtract 0x61 (decimal 97), which is lower range (lowercase 'a'),
        // which will be value 0. Then we compare (correct range should be 0 .. 25). But bytes are signed, so
        // we then do an unsigned comparison to 26 (26 letters in the alphabet, letter  'z')
        //
        // (3) Unsigned comparison:
        //      if (b2 + Byte.MIN_VALUE < 26 + Byte.MIN_VALUE) { ... we are alphabetic ... } else { we are not }
        //
        //      alternatively:
        //          Byte.compareUnsigned( b1 - 97, 26 );
        //
        // so we reduce upto 4 comparisons to 1. much less branchy.
        //
        // constants can be simplified:
        //      left: (b2 + Byte.MIN_VALUE) == (b1 + (byte)(- 97 + Byte.MIN_VALUE)) == (b1 + 31)
        //      right: (26 + Byte.MIN_VALUE) == -102
        // ...and that is the origin of our 'MAGIC' values
        //
        // total: 1 OR, 1 ADD, 1 comparison
        //
        // !! NOTE: these (byte) casts are necessary !!
        return ((byte) ((byte) (in | 0x20) + MAGIC_LC_ALPHA_OFFSET) < MAGIC_ALPHA_RANGE);
    }


    ///  True if lowercase ASCII
    static boolean isASCIILowerCase(final byte in) {
        return ((byte) (in + MAGIC_LC_ALPHA_OFFSET) < MAGIC_ALPHA_RANGE);
    }


    ///  True if ASCII ('a-f','A-F').
    private static boolean isASCIIHexLetter(final byte in) {
        // note: all casts are necessary!
        return ((byte) ((byte) (in | 0x20) + MAGIC_LC_ALPHA_OFFSET) < MAGIC_HEX_LETTER_RANGE);
    }

    ///  True if ASCII ('a-f','A-F','0-9').
    static boolean isASCIIHex(final byte b) {
        // SIMPLE:
        // return ((b >= 48 && b <= 57) ||   // 0-9
        //         (b >= 65 && b <= 70) ||   // A-F
        //         (b >= 97 && b <= 102)     // a-f
        // );
        // IMPROVED:
        return isASCIIHexLetter( b ) || isASCIIDigit( b );
    }

    /// Determine if byte is valid for a function name part (after first byte)
    /// NOTE: not for first character of a function name (only A-Z allowed for first byte)
    /// (uppercase ASCII + digits + underscore + hyphen)
    @SuppressWarnings( "unused" )
    static boolean isValidFnPart(final byte in) {
        // SIMPLE:
        // return ((b >= 65 && b <= 90) ||   // A-Z    (capitals only!)
        //         (b >= 48 && b <= 57) ||   // 0-9
        //         (b == 95 || b == 45)      // '_' or '-' (underscore, hyphen-minus)
        // );
        // IMPROVED: (the cast is necessary)
        return (
                ((byte) (in + MAGIC_CAPS_ALPHA_OFFSET) < MAGIC_ALPHA_RANGE) ||
                        isASCIIDigit( in ) ||
                        (in == 95) ||
                        (in == 45)
        );
    }

    /// True if this byte is a valid part (not the initial byte ('start')) for an identifier.
    static boolean isValidIDPart(final byte in) {
        return (
                isASCIIAlphabetic( in ) ||
                        isASCIIDigit( in ) ||
                        (in == 95) ||
                        (in == 45)
        );
    }

    /// Conversion method for error messages, debugging, etc.
    ///
    /// Special chars are escaped, and hex codes displayed
    /// adjacent to certain characters for easier debugging/improved
    /// clarity for diagnostic messages.
    ///
    /// @param in byte in
    /// @return String
    static String byteToString(final byte in) {
        // all critical characters for parsing are < 0x007F (ASCII, so within range of a signed byte)
        return switch (in) {
            case '\r' -> "<CR>";
            case '\n' -> "<LF>";
            case '\t' -> "<TAB>";   // commonly encountered but not whitespace as per fluent spec
            case ' ' -> "<WS>";     // simple whitespace (0x20); clearly indicate
            case EOF -> "<EOF>";    // our definition for an out-of-bound position
            default -> {
                if (in > 0x20 && in <= 0x7E) {
                    // printable ASCII (but not space) (note: guard patterns w/primitives not yet final in JDK)
                    // 0x7F is DEL (not printable) but spotbugs complains if we use '< 0x7F' so we are using '<= 0x7E'
                    yield "'" + (char) in + "'";
                } else {
                    // nonprintable (and not specially handled above)
                    // this will yield something like '<0x03>'
                    yield String.format( "<%#02x>", in );
                }
            }
        };
    }

    ///  Given a packed long, return the high integer (used for file position)
    static int unpackPosition(final long packedLong) {
        return (int) (packedLong >> 32);   // Hi int
    }

    ///  Given a packed long, return the low integer 
    private static int packedLow(final long packedLong) {
        return (int) (packedLong & 0xFFFFFFFFL);   // low int
    }

    ///  Given a packed long, return the low integer (used for the enum ordinal index)
    static int unpackOrdinal(final long packedLong) {
        return packedLow(packedLong);
    }

    ///  Given a packed long, return the low integer (used for the line count)
    static int unpackLineCount(final long packedLong) {
        return packedLow(packedLong);
    }

    ///  Pack the given integers into a long.
    static long packLong(final int positionHI, final int otherLOW) {
        return ((long) positionHI << 32) | (otherLOW & 0xFFFFFFFFL);
    }


    /// Calculate the 1-based line number corresponding to the current cursor position.
    ///
    /// If the current position is out-of-bounds (e.g., EOF), 0 is returned.
    ///
    /// @return the line number (>= 0); 0 indicates an invalid position
    int positionToLine() {
        return positionToLine( position() );
    }

    /// Calculate the 1-based line number for an explicit position in the stream.
    ///
    /// If the position is invalid (e.g., EOF), 0 is returned.
    ///
    /// @param position a byte offset into the decoded stream
    /// @return the line number (>= 0); 0 indicates an invalid position (typically EOF)
    int positionToLine(final int position) {
        if (position < 0 || position >= size) {
            return 0;
        }

        // simple line count
        int lfCount = 1;        // linefeed count
        for (int i = 0; i < position; i++) {
            if (seq[i] == '\n') {
                lfCount++;
            }
        }
        return lfCount;
    }

    /// length of the underlying byte array (does not including padding at end, if any)
    int length() {
        return size;
    }

    /// query position
    int position() {
        return pos;
    }

    /// true if we are not EOF
    boolean hasRemaining() {
        return (pos < size);
    }

    /// set position
    void position(int value) {
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
        return ((pos < (size - 1)) && (seq[pos + 1] == b));
    }

    /// throw exception if byte not what expected; otherwise, increment position
    /// This will also throw an exception if we are out of bounds (only positive bound checked)
    void expectChar(final byte b) {
        if (pos >= size || at() != b) {
            throw FTLParser.parseException( FTLParseException.ErrorCode.E0003,
                    FTLStream.byteToString( b ), this );
        }
        pos++;
    }

    /// consume iff match
    boolean takeCharIf(final byte b) {
        if ((pos < size) && at() == b) {
            pos++;
            return true;
        }
        return false;
    }


    int skipBlankBlock() {
        int count = 0;
        while (pos < size) {
            final int start = pos;
            skipBlankInline();
            if (!skipEOL()) {
                pos = start;
                break;
            }
            count++;
        }
        return count;
    }

    void skipBlankBlockNLC() {
        while (pos < size) {
            final int start = pos;
            skipBlankInline();
            if (!skipEOL()) {
                pos = start;
                break;
            }
        }
    }

    void skipBlank() {
        while (pos < size) {
            final byte b = seq[pos];
            if (b == ' ' || b == '\n') {
                pos++;
            } else if (b == '\r' && isNextChar( (byte) '\n' )) {
                pos += 2;
            } else {
                break;
            }
        }
    }

    int skipBlankInline() {
        final int start = pos;
        while ((pos < size) && (seq[pos] == ' ')) {
            pos++;
        }
        return (pos - start);
    }

    int getIdentifierEnd(int startIndex) {
        final int maxIndex = length();
        assert startIndex <=  maxIndex;

        if (!FTLStream.isASCIIAlphabetic( seq[startIndex] )) {
            return startIndex;
        }

        for (int i = (startIndex+1); i < maxIndex; i++) {
            if (!FTLStream.isValidIDPart( seq[i] )) {
                return i;
            }
        }
        return maxIndex;
    }


    boolean isEOL() {
        final byte b = at();
        return ((b == '\n') || (b == '\r' && isNextChar( (byte) '\n' )));
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

    ///  only used in catch() blocks during parsing, to allow parsing to continue.
    void skipToNextEntryStart() {
        byte prior = (pos > 0) ? seq[pos - 1] : (byte) '\n';
        while (pos < size) {
            final byte b = seq[pos];
            if (prior == '\n') {
                if (isASCIIAlphabetic( b ) || b == '-' || b == '#') {
                    break;
                }
            }
            pos++;
            prior = b;
        }
    }



    /// Scalar version
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
            if (isASCIIHex( b )) {
                inc();
                count++;
                codePoint *= RADIX;
                codePoint += Character.digit( b, RADIX );
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
        return isASCIIAlphabetic( at() );
    }

    boolean isBytePatternContinuation() {
        return !isLineStart( at() );
    }

    boolean isNumberStart() {
        final byte b = at();
        return (b == '-' || isASCIIDigit( b ));
    }

    void skipDigits() {
        final int start = position();
        // no need for explicit EOF check, isASCIIDigit(EOF) == false
        while (isASCIIDigit( at() )) {
            inc();
        }
        if (start == position()) {
            throw FTLParser.parseException( FTLParseException.ErrorCode.E0004,
                    "0-9",
                    this
            );
        }
    }

    /// skip to the end of line (e.g., for skipping comments).
    /// This will skip to the end of a newline, ignoring a preceding '\r' if present.
    void skipToEOL() {
        while((pos < size) && (seq[pos] != '\n')) {
           pos++;
        }
    }

    /// For debugging: return character at given position
    String dbg(final int position) {
        return "offset: " + position + ": " + byteToString( at( position ) );
    }

    /// For debugging: return character at current position
    @SuppressWarnings( "unused" )
    String dbg() {
        return dbg( position() );
    }

    ///  For debugging: display the bytes from start (inclusive) to end (exclusive)
    @SuppressWarnings( "unused" )
    String dbg(int start, int end) {
        StringBuilder sb = new StringBuilder( end - start );
        for (int i = start; i < end; i++) {
            sb.append( byteToString( seq[i] ) );
            sb.append( ' ' );
        }
        return sb.toString();
    }

}
