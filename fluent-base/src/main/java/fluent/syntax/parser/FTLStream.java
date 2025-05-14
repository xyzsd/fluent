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

import org.jspecify.annotations.NullMarked;
import org.jetbrains.annotations.Range;

/**
 * Read-only input stream used by the parser.
 * <p>
 * Contains building blocks for parser implementation.
 */
@NullMarked
public final class FTLStream {
    /*
        Design notes:
            Reading UTF8 into a ByteBuffer was originally tried, with a few variations.
            However, reading UTF8 into a String and parsing the String UTF16 representation was
            more performant in all cases. This works, because the essential characters,
            whitespace, etc. used in Fluent FTL have the same representation in both UTF8 and UTF16.

            This was pre-JDK16 (using 15), so it is possible that as things gel in JDK16/17
            the result could be different.

        Performance with char[] faster than using a CharSequence, with less variance, though
        only about 5-10%.

        methods are optimized; if we check that pos is in bounds, we can use the array
        directly. If we DO NOT check bounds, use at() instead for safety.


     */


    // constants:
    /**
     * EOF
     */
    static final char EOF = 0xFFFF;  // not a valid unicode character. Will never exist in a decoded stream
    private static final int RADIX = 16;

    // invariants:
    private final char[] seq;   // the data

    // state:
    private int pos;    // current position in stream


    /**
     * Create an FTLStream from the given String.
     * <p>
     * Streams are absolutely not threadsafe.
     * </p>
     *
     * @param in Data to parse
     * @return The stream, ready for parsing
     */
    public static FTLStream of(final String in) {
        if (in == null || in.isEmpty()) {
            throw new IllegalArgumentException( "null or zero length input" );
        }
        return new FTLStream( in.toCharArray() );
    }

    /**
     * Create an FTLStream from the given array.
     * <p>
     * The character array is treated as a properly-formed UTF16 stream.
     * <p>
     * Note that the array is used without a defensive copy being made.
     * Any modification to the array during parsing will cause undefined behavior!
     * <p>
     * Use {@code FTLStream.of(Arrays.copyOf(array, array.length))} if a defensive copy
     * is needed.
     *
     * @param array character array of length > 0
     * @return The stream, ready for parsing
     */
    public static FTLStream of(final char[] array) {
        checkArray(array);
        return new FTLStream( array );  // no defensive copy made
    }


    private static void checkArray(final char[] array)  {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException( "null or zero length array!" );
        }
    }


    /**
     * Calculate the line number in a file from the current position.
     * <p>
     * This is a shortcut for {@code positionToLine(int)}.
     * </p>
     *
     * @return line number
     */
    public int positionToLine() {
        return positionToLine( position() );
    }


    /**
     * <p>
     * For a given stream offset, calculate the line number in the file.
     * Line numbers are 1-based. However, if the offset is invalid (e.g., EOF)
     * a result of 0 will be returned.
     * </p>
     * <p>
     * We cannot reliably calculate a column number from the decoded
     * stream because the byte stream we use has already been decoded
     * from UTF8 into (Java-native) UTF16.
     * </p>
     *
     * @param position position in decoded stream
     * @return a line number >= 0 (line '0' is out-of-bounds (typically, EOF))
     */
    public int positionToLine(final int position) {
        if (position < 0 || position >= length()) {
            return 0;
        }

        int lfCount = 1;        // linefeed count
        for (int i = 0; i < position; i++) {
            if (seq[i] == '\n') {
                lfCount++;
            }
        }
        return lfCount;
    }


    // private constructor
    private FTLStream(final char[] array) {
        this.seq = array;
        this.pos = 0;
    }



    ////////////////////////////////////////////////////////////
    // simple package-private methods
    ////////////////////////////////////////////////////////////

    int length() {
        return seq.length;
    }

    int position() {
        return pos;
    }

    boolean hasRemaining() {
        return (pos < seq.length);
    }

    // set position
    // TODO: consider rename to 'setPosition' for clarity
    void position(@Range(from = 0, to = Integer.MAX_VALUE) int value) {
        pos = value;
    }

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

    /**
     * Return the char at the current position, or EOF if OOB. Does NOT change position.
     */
    char at() {
        // this is much safer and NOT really any slower than just 'return seq[pos]'
        // yay branch prediction
        return (pos < seq.length) ? seq[pos] : EOF;
    }

    /**
     * Return the char at the specified position, or EOF if out of bounds.
     */
    char at(@Range(from = 0, to = Integer.MAX_VALUE) final int position) {
        return (position < seq.length) ? seq[position] : EOF;
    }

    String subString(final int startIndex, final int endIndex) {
        return new String( seq, startIndex, (endIndex - startIndex) );
    }



    ////////////////////////////////////////////////////////////
    // static utility
    ////////////////////////////////////////////////////////////

    static boolean isLineStart(final char ch) {
        return (ch == '}' || ch == '.' || ch == '[' || ch == '*');
    }

    static boolean isASCIIDigit(final char ch) {
        return (ch >= 48 && ch <= 57);  // 0-9
    }

    static boolean isASCIIAlphabetic(final char ch) {
        return ((ch >= 97 && ch <= 122) || (ch >= 65 && ch <= 90)); // a-z || A-Z
    }

    static boolean isASCIIHexDigit(final char ch) {
        return ((ch >= 48 && ch <= 57) ||   // 0-9
                (ch >= 65 && ch <= 70) ||   // A-F
                (ch >= 97 && ch <= 102)     // a-f
        );
    }

    // determine if codePoint is valid for a function name
    // (uppercase ASCII + underscore + hyphen)
    static boolean isValidFnChar(final char ch) {
        return ((ch >= 65 && ch <= 90) ||   // A-Z
                (ch >= 48 && ch <= 57) ||   // 0-9
                (ch == 97 || ch == 45)      // '_' or '-'
        );
    }


    ////////////////////////////////////////////////////////////
    // general methods used by parser (package private)
    ////////////////////////////////////////////////////////////

    // this method is not strictly needed
    boolean isCurrentChar(final char ch) {
        return (at() == ch);
    }

    // peek at next byte (relative to current position); return true if matches
    boolean isNextChar(final char ch) {
        return ((pos < (seq.length - 1)) && (seq[pos + 1] == ch));
    }

    // throw exception if byte not what expected; otherwise, increment
    void expectChar(final char ch) {
        if (at() != ch) {
            throw ParseException.of( ParseException.ErrorCode.E0003,
                    FTLStream.toString( ch ), this );
        }
        pos++;
    }

    // consume iff match
    boolean takeCharIf(final char ch) {
        if (at() == ch) {
            pos++;
            return true;
        }
        return false;
    }

    // skip, and return # lines skipped
    int skipBlankBlock() {
        int count = 0;

        while (pos < seq.length) {
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

    void skipBlank() {
        while (pos < seq.length) {
            final char ch = seq[pos];
            if (ch == ' ' || ch == '\n') {
                pos++;
            } else if (ch == '\r' && isNextChar( '\n' )) {
                pos += 2;
            } else {
                break;
            }
        }
    }

    int skipBlankInline() {
        final int start = pos;
        while ((pos < seq.length) && (seq[pos] == ' ')) {
            pos++;
        }
        return (pos - start);
    }


    boolean isEOL() {
        final char ch = at();
        return ((ch == '\n') || (ch == '\r' && isNextChar( '\n' )));
    }


    boolean skipEOL() {
        final char ch = at();
        if (ch == '\n') {
            pos++;
            return true;
        } else if (ch == '\r' && isNextChar( '\n' )) {
            pos += 2;
            return true;
        }

        return false;
    }


    void skipToNextEntryStart() {
        char prior = (pos > 0) ? seq[pos-1] : '\n';
        while (pos < seq.length) {
            final char ch = seq[pos];
            if (prior == '\n') {
                if (isASCIIAlphabetic( ch ) || ch == '-' || ch == '#') {
                    break;
                }
            }
            pos++;
            prior = ch;
        }
    }

    /**
     * Parses a 4 or 6 byte hex sequence into a valid Unicode code point.
     * <p>
     * <b>NOTE:</b> this method returns a Unicode code point
     * </p>
     *
     * @param requiredLength 4 or 6 hex bytes
     * @return code point as an int (which is equivalent to a Java String of 1 or 2 characters)
     * @throws ParseException if hex sequence is not of the required length, or is not a valid code point.
     */
    int getUnicodeEscape(@Range(from = 4, to = 6) final int requiredLength) {
        final int start = position();
        int codePoint = 0;      // calculate in situ
        int count = 0;
        while ((count < requiredLength) && hasRemaining()) {
            final char ch = at();
            if (isASCIIHexDigit( ch )) {
                inc();
                count++;
                codePoint *= RADIX;
                codePoint += Character.digit( ch, RADIX );
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
        final char ch = at();
        return (ch == '-' || isASCIIDigit( ch ));
    }

    void skipDigits() {
        final int start = position();
        // no need for explicit EOF check, isASCIIDigit(EOF) == false
        while (isASCIIDigit( at() )) {
            inc();
        }
        if (start == position()) {
            throw ParseException.of( ParseException.ErrorCode.E0004,
                    "0-9",
                    this
            );
        }
    }

    // skip to the end of line (e.g., for skipping comments)
    void skipToEOL() {
        // newer
        while((pos < seq.length) && (seq[pos] != '\n')) {
            pos++;
        }
    }


    /**
     * Conversion method for error messages, debugging, etc.
     * <p>
     * Special chars are escaped, and hex codes displayed
     * adjacent to certain characters for easier debugging.
     * </p>
     *
     * @param in character
     * @return String
     */
    static String toString(final char in) {
        // all critical characters for parsing are < 0x007F
        return switch (in) {
            case '\r' -> "<CR>";
            case '\n' -> "<LF>";
            case '\t' -> "<TAB>";
            case ' ' -> "<WS>";     // simple whitespace (0x0020)
            case EOF -> "<EOF>";    // our definition for an out-of-bound position
            default -> {
                // for all branches below, we want to print the hex code too, (both bytes) for clarity
                if ((in > 0x0020 && in < 0x007F) || Character.isAlphabetic( in )) {
                    // todo: really want all displayable characters to 0x00FF
                    yield String.format( "'%c' (%#06x)", in, (int) in);
                } else if (Character.isWhitespace( in )) {
                    // because unicode whitespace can be deceiving; clearly delineate
                    yield String.format( "<WS:%#06x>", (int) in );
                } else {
                    // miscellaneous nonalphabetic (surrogates, control characters, etc.)
                    yield String.format( "<%#06x>", (int) in );
                }
            }
        };
    }


    /**
     * For debugging: return character at given position in the DECODED (may differ from UTF8 input) stream.
     */
    String dbg(final int position) {
        return "offset: " + position + ": " + toString( at( position ) );
    }

    /**
     * For debugging: return character at current position in decoded stream.
     */
    String dbg() {
        return dbg( position() );
    }




}
