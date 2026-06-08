package fluent.syntax.parser;

import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/// Common operations used for parsing, with a conventional, scalar implementation.
///
/// SWAR means "SIMD Within A Register": instead of using the Java Vector API, this class packs
/// eight adjacent bytes into one `long` and applies ordinary integer arithmetic/bit operations to
/// all eight byte lanes at once.
///
/// This is intended as a middle ground between scalar and SIMD (due to higher setup costs)
///
/// Because many scans are short or terminate early, SWAR could be useful. However, initial performance
/// testing results show no substantial advantage. This could depend, though, on what architecture is being used.
///
/// Lane and mask convention: bytes are loaded little-endian, so:
/// - byte lane `0` corresponds to `array[index]`;
/// - byte lane `1` corresponds to `array[index + 1]`;
/// - ...
/// - byte lane `7` corresponds to `array[index + 7]`.
///
/// Match masks use the high bit of each byte lane. For example, if lanes `0` and `3` match, the mask
/// has bits `0x0000_0000_8000_0080L` set. This allows the first matching lane to be found with
/// `Long.numberOfTrailingZeros( mask ) >>> 3`
///
/// Implementation notes:
/// We only implement `skiptoEOL`, `getIdentifierEnd`, `tsCombiner` for now.
/// For non-degenerate test cases, SWAR is faster than SIMD vector operations, and slightly faster than scalar.
/// Profiling shows that calls to other scalar functions have a width MUCH less than 8 bytes the
/// vast majority of the time. Scalar functions are also faster than SWAR for these small widths.
///
/// References:
///     - [0x80.pl](http://www.0x80.pl)
///     - [lemire.me](https://lemire.me)
///     - and others
///
@NullMarked
final class SWAROps {
    // some useful tools
    // reject non-ASCII: return ~word & HI_BITS;
    //

    // general masks
    private static final long LO_BITS = 0x0101_0101_0101_0101L;
    private static final long HI_BITS = 0x8080_8080_8080_8080L;

    // ASCII masks
    private static final long LF_MASK = broadcast( '\n' );
    private static final long SPACE_MASK = broadcast( ' ' );
    private static final long CR_MASK = broadcast( '\r' );
    private static final long ASCII_CASE_MASK = broadcast( 0x20 );
    private static final long UNDERSCORE_MASK = broadcast( '_' );
    private static final long HYPHEN_MASK = broadcast( '-' );
    private static final long LC_A = broadcast( 'a' );
    private static final long AFTER_LC_Z = broadcast( 'z' + 1 );
    private static final long DIGIT_0 = broadcast( '0' );
    private static final long AFTER_DIGIT_9 = broadcast( '9' + 1 );


    // extends byte b across all lanes (e.g., to create a mask).
    private static long broadcast(final int b) {
        // & 0xFF to prevent sign extension
        return (b & 0xFFL) * LO_BITS;
    }

    // reject non-ASCII bytes
    private static long rejectNonASCII(final long word) {
        // only ASCII bytes are returned
        return ~word & HI_BITS;
    }

    // any matching byte will have a '0x80' in that lane
    private static long zeroByteMask(final long word) {
        return (word - LO_BITS) & ~word & HI_BITS;
    }

    // any matching byte will have a '0x80' in that lane
    private static long eqByteMask(final long word, final long mask) {
        return zeroByteMask( word ^ mask );
    }

    // any byte lane less than n will have a 0x80 in that lane.
    // n must be in the range 1..128. (word | HI_BITS) ensures lane range is 0x80->0xFF so we don't
    // borrow from neighboring lanes when we subtract.
    private static long lessThanByteMask(final long word, final long mask) {
        return ~((word | HI_BITS) - mask) & HI_BITS;
    }

    // any ASCII byte lane in the inclusive range [lo, hi] will have a 0x80 in that lane.
    private static long asciiRangeMask(final long word, final long loMask, final long hiMask) {
        final long geLo = ~lessThanByteMask( word, loMask ) & HI_BITS;
        final long ltAfterHi = lessThanByteMask( word, hiMask  );
        return rejectNonASCII( word ) & geLo & ltAfterHi;
    }


    // UTF8 is not byte-order dependent .... just bytes
    // so we can use LE, which means LSB is lowest (and MSB highest) array index
    // useful for using with Long.numberOfTrailingZeros() >>> 3 to get the byte lane

    // load bytes from an array into a long.
    // NOTE: limit should be array length - Long.BYTES so we don't
    // usage: long word = (long) LONG_LE.get( buf, index );
    private static final VarHandle LONG_LE =
            MethodHandles.byteArrayViewVarHandle( long[].class, ByteOrder.LITTLE_ENDIAN );


    static int skipToEOL(final byte[] array, final int startIndex) {
        int i = startIndex;

        final int longLimit = array.length - Long.BYTES;
        for (; i <= longLimit; i += Long.BYTES) {
            final long word = (long) LONG_LE.get( array, i );
            final long lnFeeds = eqByteMask( word, LF_MASK );

            if (lnFeeds != 0) {
                return i + (Long.numberOfTrailingZeros( lnFeeds ) >>> 3);
            }
        }

        // process remaining bytes as scalar
        for (; i < array.length; i++) {
            if (array[i] == '\n') {
                return i;
            }
        }

        return array.length;
    }

    static int getIdentifierEnd(final byte[] array, final int startPos) {
        if (!CommonOps.isASCIIAlphabetic( array[startPos] )) {
            return startPos;
        }

        int i = startPos + 1;

        final int longLimit = array.length - Long.BYTES;
        for (; i <= longLimit; i += Long.BYTES) {
            final long word = (long) LONG_LE.get( array, i );

            final long folded = word | ASCII_CASE_MASK;
            final long alphabetic = asciiRangeMask( folded, LC_A, AFTER_LC_Z );
            final long digits = asciiRangeMask( word, DIGIT_0, AFTER_DIGIT_9 );
            final long underscores = eqByteMask( word, UNDERSCORE_MASK );
            final long hyphens = eqByteMask( word, HYPHEN_MASK );

            final long valid = alphabetic | digits | underscores | hyphens;
            final long invalid = ~valid & HI_BITS;

            if (invalid != 0) {
                return i + (Long.numberOfTrailingZeros( invalid ) >>> 3);
            }
        }

        for (; i < array.length; i++) {
            if (!CommonOps.isValidIDPart( array[i] )) {
                return i;
            }
        }

        return array.length;
    }


    /// For TextSlices, creates a bitmask to identify any of the characters '{', '}', '\n', and '\r',
    /// and produces a combined result using bitwise OR operations.
    ///
    /// NOTE: This will load 8 bytes (as a long) starting at the given position in the array.
    ///
    static long tsCombiner(final byte[] array, final int pos) {
        final long word = (long) LONG_LE.get( array, pos );

        final long openMask = eqByteMask( word, '{' );
        final long closeMask = eqByteMask( word, '}' );
        final long lfMask = eqByteMask( word, '\n' );
        final long crMask = eqByteMask( word, '\r' );

        return openMask | closeMask | lfMask | crMask;
    }


    /// helper for getTextSlice (SWAR version)
    static boolean isBlank(final byte[] array, final int startPos, final int endPos) {
        return (skipBlankRanged( array, startPos, endPos ) == endPos);
    }

    private static int skipBlankRanged(final byte[] array, final int startPos, final int endPos) {
        int i = startPos;

        final int longLimit = endPos - Long.BYTES;
        for (; i <= longLimit; i += Long.BYTES) {
            final long word = (long) LONG_LE.get( array, i );

            final long spaces = eqByteMask( word, SPACE_MASK );
            final long lineFeeds = eqByteMask( word, LF_MASK );
            final long carriageReturns = eqByteMask( word, CR_MASK );

            // Since loadLongLE is little-endian, byte lane 0 is the lowest-addressed byte.
            // A LF at lane N+1 corresponds to a CRLF pair starting at lane N.
            final long crlfWithinWord = carriageReturns & (lineFeeds >>> Byte.SIZE);

            // Handle CR at lane 7 followed by LF at the next array position.
            final long crlfAcrossBoundary = ((carriageReturns & 0x8000_0000_0000_0000L) != 0
                    && i + Long.BYTES < endPos
                    && array[i + Long.BYTES] == (byte) '\n')
                    ? 0x8000_0000_0000_0000L
                    : 0L;

            // Mark byte lanes that are valid Fluent whitespace.
            // For CRLF, the CR byte is marked by crlfWithinWord/crlfAcrossBoundary,
            // and the LF byte is already marked by lineFeeds.
            final long blanks = spaces | lineFeeds | crlfWithinWord | crlfAcrossBoundary;

            // High bit set where byte is not recognized as blank.
            final long nonBlanks = ~blanks & HI_BITS;

            if (nonBlanks != 0) {
                return i + (Long.numberOfTrailingZeros( nonBlanks ) >>> 3);
            }
        }

        // remember: limit is not end of array, but 'endPos'
        while (i < endPos) {
            final byte b = array[i];
            if (b == ' ' || b == '\n') {
                i++;
            } else if (b == '\r' && i < (endPos - 1) && array[i + 1] == (byte) '\n') {
                i += 2;
            } else {
                break;
            }
        }

        return i;
    }

}
