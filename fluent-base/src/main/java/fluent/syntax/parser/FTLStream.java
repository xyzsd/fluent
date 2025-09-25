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

/// Read-only UTF-8 input over FTL source used by the parser.
/// FTLStream wraps a byte array or buffer and provides performant primitives tailored to
/// the Fluent syntax parsing. The underlying data is immutable; a lightweight cursor (position)
/// advances as the parser consumes input.
///
/// Implementation note: for methods that take a ByteBuffer or byte array, no copy of the array (or backing array)
/// is made if the vectorized (SIMD) implementation is used. If the SIMD implementation is not active, the backing
/// array is copied.
///
@NullMarked
public final class FTLStream {

    // EOF (0xFF) is not a valid Unicode character, and should
    // not be present in a stream unless malformed.
    static final byte EOF = (byte) 0xFF;

    //  set of functions to use for accelerated operations (SIMD vs. SWAR)
    private static final Accel ACCEL = Accel.get();

    // magic constants for ASCII comparisons.
    private static final byte MAGIC_CAPS_ALPHA_OFFSET = ((byte) (-65 + Byte.MIN_VALUE)); // 'A' (uppercase A)
    private static final byte MAGIC_LC_ALPHA_OFFSET = ((byte) (-97 + Byte.MIN_VALUE)); // 'a' (lowercase A)
    private static final byte MAGIC_ALPHA_RANGE = ((byte) (26 + Byte.MIN_VALUE));  // 26 letters

    private static final byte MAGIC_HEX_LETTER_RANGE = ((byte) (6 + Byte.MIN_VALUE));  // 6 letter range
    private static final byte MAGIC_DIGIT_RANGE = ((byte) (10 + Byte.MIN_VALUE));    // 10 digit range
    private static final byte MAGIC_DIGIT_OFFSET = ((byte) (-48 + Byte.MIN_VALUE));    // '0' ascii value

    // internal
    private static final int RADIX = 16;

    // invariants
    private final byte[] seq;   // UTF8 octets. seq.length == (size + ACCEL.pad())
    private final int size;     // size of actual data in seq[]

    // state
    private int pos;    // current position in stream


    // private constructor
    private FTLStream(final byte[] array) {
        this.size = array.length;
        this.pos = 0;

        // only copy (to add padding) if we are using SWAR.
        if (ACCEL.isVector()) {
            // SIMD
            seq = array;
        } else {
            // SWAR (padding required)
            seq = new byte[size + ACCEL.pad()];
            for (int i = array.length; i < seq.length; i++) {
                seq[i] = EOF;
            }
            System.arraycopy( array, 0, seq, 0, array.length );
        }
    }

    /// Create a stream from a String.
    ///
    /// The string is encoded as UTF‑8. This is convenient for tests or very small inputs, but is not efficient.
    /// For most inputs, prefer byte[]/ByteBuffer or resource loading.
    ///
    /// @param in non-empty source text
    /// @return a new FTLStream over the provided text
    /// @throws IllegalArgumentException if the input is empty
    public static FTLStream of(final String in) {
        requireNonNull( in );
        if (in.isEmpty()) {
            throw new IllegalArgumentException( "empty input String!" );
        }
        return new FTLStream( in.getBytes( StandardCharsets.UTF_8 ) );
    }

    /// Create a stream from a byte array.
    ///
    /// If vector instructions (SIMD) are enabled, no copy of the input array is performed.
    /// If any mutation of the input array occurs during parsing, parse behavior is undefined.
    /// The input array must be non-empty.
    ///
    /// Input bytes are interpreted as UTF‑8.
    ///
    /// A copy of the input is made if and only if SIMD instructions are disabled.
    ///
    /// @param array the UTF‑8 encoded bytes of FTL source
    /// @return a new FTLStream
    /// @throws IllegalArgumentException if the array is empty
    public static FTLStream of(final byte[] array) {
        requireNonNull( array, "null array" );
        if (array.length == 0) {
            throw new IllegalArgumentException( "Zero length array!" );
        }

        return new FTLStream( array );
    }

    /// Create a stream from a ByteBuffer.
    ///
    /// This stream will reference the backing array of the buffer. The buffer must have a non-zero limit.
    ///
    /// @param bb a ByteBuffer whose remaining bytes contain UTF‑8 encoded FTL source
    /// @return a new FTLStream over the buffer's backing array
    /// @throws IllegalArgumentException         if the buffer has zero length
    /// @throws java.nio.ReadOnlyBufferException if the buffer is backed by an array but is read-only
    /// @throws UnsupportedOperationException    if the buffer is not backed by an accessible array
    public static FTLStream of(final ByteBuffer bb) {
        requireNonNull( bb );
        if (bb.limit() == 0) {
            throw new IllegalArgumentException( "ByteBuffer of zero length!" );
        }

        return new FTLStream( bb.array() );  // no defensive copy made
    }

    /// Create a stream by reading an FTL resource from the classpath.
    ///
    /// Example:
    /// {@snippet :
    ///     FTLStream.from(
    ///         Thread.currentThread().getContextClassLoader(),
    ///         "fluent/examples/hello/en-US/main.ftl"
    ///     );
    ///}
    ///
    /// @param classLoader a class loader (e.g., Thread.currentThread().getContextClassLoader())
    /// @param resource    the classpath resource to load
    /// @return a new FTLStream for the resource contents
    /// @throws IOException           if an I/O error occurs while reading
    /// @throws FileNotFoundException if the resource cannot be found
    public static FTLStream from(final ClassLoader classLoader, final String resource)
            throws IOException {
        requireNonNull( classLoader );
        requireNonNull( resource );

        try (InputStream is = classLoader.getResourceAsStream( resource )) {
            if (is == null) {
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
        // all critical characters for parsing are < 0x007F (ASCII)
        return switch (in) {
            case '\r' -> "<CR>";
            case '\n' -> "<LF>";
            case '\t' -> "<TAB>";   // commonly encountered but not whitespace as per fluent spec
            case ' ' -> "<WS>";     // simple whitespace (0x20); clearly indicate
            case EOF -> "<EOF>";    // our definition for an out-of-bound position
            default -> {
                if (in > 0x20 && in < 0x7F) {
                    // printable ASCII (but not space) (note: guard patterns w/primitives not yet final in JDK)
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
    static int position(final long packedLong) {
        return (int) (packedLong >> 32);   // Hi int
    }

    ///  Given a packed long, return the low integer (used for the enum ordinal index)
    static int ordinal(final long packedLong) {
        return (int) (packedLong & 0xFFFFFFFFL);   // low int
    }

    ///  Pack the given integers into a long.
    static long packLong(final int positionHI, final int ordinalLO) {
        return ((long) positionHI << 32) | (ordinalLO & 0xFFFFFFFFL);
    }

    /// True if using vectorization; false if SWAR
    static boolean isSIMD() {
        return ACCEL.isVector();
    }

    /// Calculate the 1-based line number corresponding to the current cursor position.
    ///
    /// If the current position is out-of-bounds (e.g., EOF), 0 is returned.
    ///
    /// @return the line number (>= 0); 0 indicates an invalid position
    public int positionToLine() {
        return positionToLine( position() );
    }

    /// Calculate the 1-based line number for an explicit position in the stream.
    ///
    /// If the position is invalid (e.g., EOF), 0 is returned.
    ///
    /// @param position a byte offset into the decoded stream
    /// @return the line number (>= 0); 0 indicates an invalid position (typically EOF)
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
            throw FTLParser.parseException( ParseException.ErrorCode.E0003,
                    FTLStream.byteToString( b ), this );
        }
        pos++;
    }

    /// consume iff match
    boolean takeCharIf(final byte b) {
        if (at() == b) {
            pos++;
            return true;
        }
        return false;
    }

    ///  Skip a blank block.
    ///  We first skip inline. But if the next character is an EOL, stop and rewind.
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

    /// Skip blank blocks, but without counting of the number of lines skipped.
    void skipBlankBlockNLC() {
        // this is not yet vectorized; essentially a placeholder.
        while (pos < size) {
            final int start = pos;
            skipBlankInline();
            if (!skipEOL()) {
                pos = start;
                break;
            }
        }
    }

    ///  Skip over blank space (WS, \n, \r\n) until we hit something.
    /// The position is advanced to that point.
    void skipBlank() {
        position( ACCEL.skipBlank( seq, pos ) );
        // SCALAR VERSION:
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
        final int newPos = ACCEL.skipBlankInline( seq, pos );
        final int start = pos;
        pos = newPos;
        return (newPos - start);
        // SCALAR VERSION:
        //
        // final int start = pos;
        // while ((pos < size) && (seq[pos] == ' ')) {
        //    pos++;
        // }
        // return (pos - start);
        //
    }

    ///  From startIndex, find the end of a legal identifier.
    ///  If the returned value is the same as startIndex, the identifier is invalid
    ///  (illegal initial character)
    int getIdentifierEnd(final int startIndex) {
        return ACCEL.getIdentifierEnd( seq, startIndex );
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

    ///  Vectorized implementation for getTextSlice()
    long nextTSChar(int startIndex) {
        return SIMD.nextTSChar( seq, startIndex );
    }

    ///  Determine if the given range is only Fluent-whitespace (ASCII Space, LF, or CR-LF pair)
    boolean isBlank(int startIndex, int endIndex) {
        return ACCEL.isBlank( seq, startIndex, endIndex );
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
        pos = ACCEL.nextLF( seq, pos );
        // SCALAR VERSION:
        //while((pos < size) && (seq[pos] != '\n')) {
        //   pos++;
        //}
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

    /// Return a concise diagnostic string describing this instance.
    @Override
    public String toString() {
        return "FTLStream{" +
                "size=" + size +
                ", accel = " + ACCEL +
                '}';
    }
}
