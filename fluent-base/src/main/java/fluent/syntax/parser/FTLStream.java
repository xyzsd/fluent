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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

/// Read-only input stream used by the parser.
///
/// Contains building blocks for parser implementation.
@NullMarked
public final class FTLStream {

    // EOF (0xFF) is not a valid Unicode character, and should
    // not be present in a stream unless malformed.
    static final byte EOF = (byte) 0xFF;

    // magic constants for ascii comparisons.
    private static final byte MAGIC_CAPS_ALPHA_OFFSET = ((byte) (-65 + Byte.MIN_VALUE)); // 'A' (uppercase A)
    private static final byte MAGIC_LC_ALPHA_OFFSET = ((byte) (-97 + Byte.MIN_VALUE)); // 'a' (lowercase A)
    private static final byte MAGIC_ALPHA_RANGE = ((byte) (26 + Byte.MIN_VALUE));  // 26 letters

    private static final byte MAGIC_HEX_LETTER_RANGE = ((byte) (6 + Byte.MIN_VALUE));  // 6 letter range
    private static final byte MAGIC_DIGIT_RANGE = ((byte) (10 + Byte.MIN_VALUE));    // 10 digit range
    private static final byte MAGIC_DIGIT_OFFSET = ((byte) (-48 + Byte.MIN_VALUE));    // '0' ascii value

    // internal
    private static final int RADIX = 16;

    // invariants:
    private final byte[] seq;   // UTF8 octets. seq.length == (size + SWAR.PAD)
    private final int size;     // size of actual data in seq[]

    // state:
    private int pos;    // current position in stream


    // private constructor
    private FTLStream(final byte[] array) {
        this.size = array.length;
        this.pos = 0;

        seq = new byte[size + SWAR.PAD];
        for (int i = array.length; i < seq.length; i++) {
            seq[i] = EOF;
        }
        System.arraycopy( array, 0, seq, 0, array.length );
    }

    // NOT efficient, but useful for very short things/testing
    public static FTLStream of(final String in) {
        requireNonNull( in );
        if (in.isEmpty()) {
            throw new IllegalArgumentException( "empty input String!" );
        }
        return new FTLStream( in.getBytes( StandardCharsets.UTF_8 ) );
    }

    // we copy
    public static FTLStream of(final byte[] array) {
        requireNonNull( array, "null array" );
        if (array.length == 0) {
            throw new IllegalArgumentException( "Zero length array!" );
        }

        return new FTLStream( array );
    }

    // TODO: verify endianness/SWAR
    public static FTLStream of(final ByteBuffer bb) {
        requireNonNull( bb );
        if (bb.limit() == 0) {
            throw new IllegalArgumentException( "ByteBuffer of zero length!" );
        }

        return new FTLStream( bb.array() );  // no defensive copy made
    }

    /// Read directly from a resource stream.
    ///
    /// @param classLoader such as Thread.currentThread().getContextClassLoader();
    /// @param resource    resource to load
    /// @return FTLStream
    /// @throws IOException if resource is missing or an I/O error occurs while reading.
    public static FTLStream from(final ClassLoader classLoader, final String resource)
            throws IOException {
        requireNonNull( classLoader );
        requireNonNull( resource );

        try (InputStream is = classLoader.getResourceAsStream( resource )) {
            if (is == null) {
                // todo: exception choice
                throw new FileNotFoundException( resource );
            }

            return new FTLStream( is.readAllBytes() );
        }
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
        // simple approach: many comparisons
        // return ((in >= 97 && in <= 122) || (in >= 65 && in  <= 90)); // a-z || A-Z
        //
        // fancy approach:
        // (1) byte b1 = in | 0x20; // convert uppercase to lowercase ASCII by flipping this bit
        //      now we only have 1 range to check (lowercase 'a .. z')
        // (2) byte b2 = (b1 - 97); subtract 0x61 (dec 97), which is lower range (lowercase 'a'), which will be value 0
        // then we compare (correct range should be 0 .. 25), but bytes are signed, so
        // we then do an unsigned comparison to 26 (26 letters in the alphabet, letter 'z') like so:
        // (3) if (b2 + Byte.MIN_VALUE < 26 + Byte.MIN_VALUE) { ... we are alphabetic ... } else { we are not }
        // so we reduce upto 4 comparisons to 1. much less branchy.
        // constants can be simplified:
        //      left: (b2 + Byte.MIN_VALUE) == (b1 + (byte)(- 97 + Byte.MIN_VALUE)) == (b1 + 31)
        //      right: (25 + Byte.MIN_VALUE) == -103
        // ...and that is the origin of our 'MAGIC' values
        //
        // total: 1 OR, 1 ADD, 1 comparison
        //
        // future: could do a SWAR/SIMD approach, particularly for identifiers
        //
        // !! NOTE: these (byte) casts are necessary !!
        return ((byte) ((byte) (in | 0x20) + MAGIC_LC_ALPHA_OFFSET) < MAGIC_ALPHA_RANGE);
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

    /// determine if byte is valid for a function name
    /// NOTE: not for first character of a function name (only A-Z allowed for first byte)
    /// (uppercase ASCII + digits + underscore + hyphen)
    static boolean isValidFnChar(final byte in) {
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

    /// Conversion method for error messages, debugging, etc.
    ///
    /// Special chars are escaped, and hex codes displayed
    /// adjacent to certain characters for easier debugging/improved
    /// clarity for diagnostic messages.
    ///
    /// @param in byte in
    /// @return String
    static String byteToString(final byte in) {
        // all critical characters for parsing are < 0x007F (ASCII)
        return switch (in) {
            case '\r' -> "<CR>";
            case '\n' -> "<LF>";
            case '\t' -> "<TAB>";   // commonly encountered but not whitespace as per fluent spec
            case ' ' -> "<WS>";     // simple whitespace (0x20)
            case EOF -> "<EOF>";    // our definition for an out-of-bound position
            default -> {
                if (in > 0x20 && in < 0x7F) {
                    // printable ASCII (but not space) (note: guard patterns w/primitives not yet final in JDK)
                    yield "'" + (char) in + "'";
                } else {
                    // nonprintable (and not specially handled above)
                    // this will yield something like '<0x03>'
                    yield String.format( "<%#02x>", (int) in );
                }
            }
        };
    }

    /// Calculate the line number in a file from the current position.
    ///
    public int positionToLine() {
        return positionToLine( position() );
    }

    ///
    /// For a given stream offset, calculate the line number in the file.
    /// Line numbers are 1-based. However, if the offset is invalid (e.g., EOF)
    /// a result of 0 will be returned.
    ///
    /// @param position position in decoded stream
    /// @return a line number >= 0 (line '0' is out-of-bounds (typically, EOF))
    public int positionToLine(final int position) {
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

    // length of actual buffer (not pad)
    int length() {
        return size;
    }

    int position() {
        return pos;
    }

    boolean hasRemaining() {
        return (pos < size);
    }

    // set position
    // TODO: consider rename to 'setPosition' for clarity
    void position(int value) {
        pos = value;
    }


    // //////////////////////////////////////////////////////////
    // static utility
    // //////////////////////////////////////////////////////////

    void inc() {
        pos += 1;
    }

    void inc(final int increment) {
        pos += increment;
    }

    void dec() {
        assert this.pos > 0;
        pos -= 1;
    }

    void dec(final int decrement) {
        assert pos > decrement;
        pos -= decrement;
    }

    /// Return the byte at the current position, or EOF if OOB. Does NOT change position.
    /// *NOTE*: this will fail if negative or more than padding.
    byte at() {
        return seq[pos];
        // this is much safer and NOT really any slower than just 'return seq[pos]'
        // yay branch prediction
        //return (pos < size) ? seq[pos] : EOF;
    }

    /// Return the byte at the specified position, or EOF if out of bounds.
    /// Fail (exception) if negative
    byte at(final int position) {
        return seq[position];
        //return (position < size) ? seq[position] : EOF;
    }


    // //////////////////////////////////////////////////////////
    // general methods used by parser (package private)
    // //////////////////////////////////////////////////////////

    String subString(final int startIndex, final int endIndex) {
        /* alternate approach: keep decoder as a static final class var and then:
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.reset();
        decoder.decode( ByteBuffer.wrap( seq, startIndex, (endIndex-startIndex) )).toString();
        */
        return new String( seq, startIndex, (endIndex - startIndex), StandardCharsets.UTF_8 );
    }

    /// this method is not strictly needed
    boolean isCurrentChar(final byte b) {
        return (at() == b);
    }


    // TODO: isNextChar() : eliminate size check ? we are padded
    /// peek at next byte (relative to current position); return true if matches
    /// DOES NOT increment position
    boolean isNextChar(final byte b) {
        return ((pos < (size - 1)) && (seq[pos + 1] == b));
    }

    /// throw exception if byte not what expected; otherwise, increment position
    void expectChar(final byte b) {
        if (at() != b) {
            throw FTLParser.parseException( ParseException.ErrorCode.E0003,
                    FTLStream.byteToString( b ), this );
        }
        pos++;
    }

    // consume iff match
    boolean takeCharIf(final byte b) {
        if (at() == b) {
            pos++;
            return true;
        }
        return false;
    }

    // skip, and return # lines skipped
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


    ///  Skip over blank space (WS, \n, \r\n) until we hit something.
    /// The position is advanced to that point.
    void skipBlank() {
        // NEW, SWAR version
        position( SWAR.skipBlank( seq, pos ) );

        // OLD, simple version
        //
        // while (pos < size) {
        //     final byte b = seq[pos];
        //     if (b == ' ' || b == '\n') {
        //         pos++;
        //     } else if (b == '\r' && isNextChar( (byte) '\n' )) {
        //         pos += 2;
        //     } else {
        //         break;
        //     }
        // }
    }

    ///  skip whitespace (which is only the ASCII space character (0x20))
    ///
    /// We only use the return value when we check indent.
    int skipBlankInline() {
        final int newPos = SWAR.skipBlankInline( seq, pos );
        // old version:
        // final int start = pos;
        // while ((pos < size) && (seq[pos] == ' ')) {
        //    pos++;
        // }
        // return (pos - start);
        //
        final int start = pos;
        pos = newPos;
        return (newPos - start);
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

    /// Parses a 4 or 6 byte hex sequence into a valid Unicode code point.
    ///
    /// *NOTE:* this method returns a Unicode code point (as an int)
    ///
    /// @param requiredLength 4 or 6 hex bytes
    /// @return code point as an int (which is equivalent to a Java String of 1 or 2 characters)
    /// @throws ParseException if hex sequence is not of the required length, or is not a valid code point.
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
            throw ParseException.of(
                    ParseException.ErrorCode.E0026,
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
            throw FTLParser.parseException( ParseException.ErrorCode.E0004,
                    "0-9",
                    this
            );
        }
    }

    /// skip to the end of line (e.g., for skipping comments).
    /// This will skip to the end of a newline, ignoring a preceding '\r' if present.
    void skipToEOL() {
        pos = SWAR.nextLF( seq, pos );
        // original
        //while((pos < size) && (seq[pos] != '\n')) {
        //   pos++;
        //}
    }

    /// For debugging: return character at given position
    String dbg(final int position) {
        return "offset: " + position + ": " + byteToString( at( position ) );
    }

    /// For debugging: return character at current position in decoded stream.
    String dbg() {
        return dbg( position() );
    }


}
