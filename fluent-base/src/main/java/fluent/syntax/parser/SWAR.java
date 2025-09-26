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

package fluent.syntax.parser;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

///  [SWAR](https://en.wikipedia.org/wiki/SWAR) Methods and utilities
///
///  All SWAR operations here will use a long, with 8-bit lanes. (8 lanes (bytes) per long)
///
/// There are some methods marked *private* in this class which are potentially useful
/// but as yet unused.
///
///  Methods in this class are implementations of (or inspired by) ideas from the following:
///     - [Daniel Lemire (blog)](https://lemire.me/blog/?s=swar)
///     - [Wojciech Muła](http://0x80.pl/notesen-swar.html)
final class SWAR {

    ///  amount the buffer is padded (8 bytes typical)
    ///  if we start on LAST byte, and a long is 8 bytes, we need 7 more bytes.
    ///  however if we have a +1 offset for (say) CR-LF detection, we need 8 more bytes.
    static final int PAD = 8;

    /// Hi bits. Also useful for determining if a byte (UTF-8) is ASCII or not.
    /// ASCII bytes will not have the high bit set.
    private static final long HI_BITS = 0x8080808080808080L;
    ///  Low bits mask ('one mask')
    private static final long LO_BITS = 0x0101010101010101L;
    ///  7F mask (7 bits)
    private static final long MASK_7F = 0x7F7F7F7F7F7F7F7FL;
    /// ///  LF (linefeed)
    private static final long LF_MASK = 0x0a0a0a0a0a0a0a0aL;
    ///  CR (carriage return)
    private static final long CR_MASK = 0x0d0d0d0d0d0d0d0dL;
    ///  Space (ASCII space)
    private static final long SPC_MASK = 0x2020202020202020L;
    ///  Mask off final byte (ignore 8th byte)
    private static final long MASK_LAST_BYTE = 0xFF_FF_FF_FF_FF_FF_FF_00L;
    ///  All bits set
    private static final long ALL_BITS = 0xFF_FF_FF_FF_FF_FF_FF_FFL;

    ///  View a byte array as a long. Easier and more performant way to fill a Long with 8 bytes.
    ///  NOTE: Be wary of endianness depending upon the byte buffer used
    private static final VarHandle LONGVIEW = MethodHandles.byteArrayViewVarHandle(
            long[].class, ByteOrder.BIG_ENDIAN );


    private SWAR() {}


    ///  Currently this is scalar
    static int getIdentifierEnd(final byte[] buf, final int startIndex) {
        // SCALAR version:
        final int maxIndex = buf.length - PAD;
        assert startIndex <  maxIndex;

        if (!FTLStream.isASCIIAlphabetic( buf[startIndex] )) {
            return startIndex;
        }

        for (int i = (startIndex+1); i < maxIndex; i++) {
            if (!FTLStream.isValidIDPart( buf[i] )) {
                return i;
            }
        }
        return maxIndex;
    }


    ///  Find position of next LF in buffer, or last position in buf. ASSUMES BUFFER IS PADDED
    static int nextLF(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length - PAD;
        for (int i = startIndex; i < maxIndex; i += 8) {
            final long input = (long) LONGVIEW.get( buf, i );
            long is_ascii = HI_BITS & (~input);
            long subLF = input ^ LF_MASK;
            long eqLF = (subLF - LO_BITS);
            long eqLFandIsAscii = (eqLF & is_ascii);
            if (eqLFandIsAscii != 0) {
                // leading/trailing zeros functions are intrinsics
                // returns 0-63 if found, 64 if not found (all zeros)
                // right shift by 3 to convert to byte position
                return (Long.numberOfLeadingZeros( eqLFandIsAscii ) >> 3) + i;
            }
        }
        return maxIndex;    // not found; we are at EOF
    }

    ///  Given a block of bytes (from startPos to endPos),
    ///  are these bytes ONLY whitespace (LF, ' ' (ASCII 0x20), CRLF (paired)) ?
    ///
    ///  Currently this is a scalar implementation
    static boolean isBlank(final byte[] buf, final int startIndex, final int endIndex) {
        // TODO: SWAR vectorization; similar to skipBlank.
        //          may be able to use skipBlank() but with an additional bound
        //          and differnet test (need to make sure we are not past endIndex)
        //
        // SCALAR VERSION FOLLOWS:
        boolean priorIsNewline = false;
        for(int i=endIndex-1; i>=startIndex; i--) {
            final byte b = buf[i];
            if (b == ' ' || (priorIsNewline && b == '\r')) {
                priorIsNewline = false;
            } else if (b == '\n') {
                priorIsNewline = true;
            } else {
                // not blank!
                return false;
            }
        }
        return true;
    }


    // not used. different implementation
    @SuppressWarnings( "unused" )
    private static int nextLF_V2(final byte[] buf, final int index) {
        final int maxIndex = buf.length - PAD;
        for (int i = index; i < maxIndex; i += 8) {
            final long input = (long) LONGVIEW.get( buf, i );
            final long lo7bits = input & MASK_7F;
            final long lf = (lo7bits ^ LF_MASK) + MASK_7F;
            final long t = ((lf | input) & HI_BITS) ^ HI_BITS;

            if (t != 0) {
                return (Long.numberOfLeadingZeros( t ) >> 3) + i;
            }
        }
        return maxIndex;    // not found; we are at EOF
    }


    ///  create a mask (long) from a byte
    @SuppressWarnings( "unused" )
    private static long broadcast(final byte b) {
        return 0x101010101010101L * b;
    }

    /// As used in jdk.incubator.vector documentation; 'round down'
    @SuppressWarnings( "unused" )
    private static int loopBound(final int arrayLength) {
        // more general form:
        // SPECIES_LENGTH = 8 (for a long, 8 bytes). For SIMD vectors, this would be longer
        // bound = (arrayLength & ~(SPECIES_LENGTH - 1))
        // While the compiler should do this for us, given we are only working with long (8 bytes) here:
        // bound = (arrayLength & ~(8 - 1))
        // bound = (arrayLength & ~(7));    ~7 == -8
        // bound = (arrayLength & -8)
        return (arrayLength & (-8));
    }

    /// Sort of the opposite of loopBound. Terrible name.
    @SuppressWarnings( "unused" )
    private static int roundUpBy8B(final int size) {
        // More general form:
        // SPECIES_LENGTH = 8;
        // roundUP = (size + (SPECIES_LENGTH -1)) & ~(SPECIES_LENGTH -1)
        //
        return (size + 7) & (-8);
    }


    ///  for debugging
    @SuppressWarnings( "unused" )
    private static String bin(byte in) {
        String s = Integer.toBinaryString( in );
        if (s.length() <= 8) {
            return "0".repeat( 8 - s.length() ) + s;
        }
        return s;
    }


    ///  For debugging
    @SuppressWarnings( "unused" )
    private static String toHex(long l) {
        String s = Long.toUnsignedString( l, 16 );
        int pad = 16 - s.length();
        return ("0".repeat( pad ) + s);
    }

    ///  Find the first non-space (ASCII whitespace, 0x20)
    static int skipBlankInline(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length - PAD;
        for (int i = startIndex; i < maxIndex; i += 8) {
            final long input = (long) LONGVIEW.get( buf, i );
            // long version
            // (modified delimiter check)
            //
            // final long lo7bits = input & MASK_7F;
            // final long d0 = (lo7bits ^ SPC_MASK) + MASK_7F;
            // final long t0 = d0 | input;
            //
            // all spaces are 0, all non-spaces have the high bit set (0x80)
            // final long t1 = t0 & HI_BITS;
            //
            final long t1 = ((((input & MASK_7F) ^ SPC_MASK) + MASK_7F) | input) & HI_BITS;

            if (t1 != 0) {
                // leading/trailing zeros functions are intrinsics
                // returns 0-63 if found, 64 if not found (all zeros)
                // right shift by 3 to convert to byte position
                return (Long.numberOfLeadingZeros( t1 ) >> 3) + i;
            }
        }
        return maxIndex;    // not found; we are at EOF
    }


    ///  Skip blanks.
    ///
    ///  This skips contiguous runs of ASCII Space (0x20), LF (0x0a), and CR-LF pairs (0x0d,0x0a).
    ///  Unpaired CRs are not skipped.
    static int skipBlank(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length - PAD;
        // NOTE: increment is 7 here! important!
        // basically an 8 byte window with 1 byte overlap
        for (int i = startIndex; i < maxIndex; i += 7) {
            final long in = (long) LONGVIEW.get( buf, i );

            long isAscii = HI_BITS & ~in;
            long eqLF = (in ^ LF_MASK) - LO_BITS;
            // we match all bytes, but we only care about the first 7 bytes here
            long eqSpace = (in ^ (SPC_MASK)) - LO_BITS;
            long eqCR = (in ^ (CR_MASK)) - LO_BITS;
            long eqCRLF = (eqLF << 8) & eqCR;
            // result : MUST mask off last byte!
            long result = ((isAscii & (eqLF | eqSpace | eqCRLF)) ^ HI_BITS) & MASK_LAST_BYTE;

            if (result != 0) {
                return (Long.numberOfLeadingZeros( result ) >> 3) + i;
            }
        }
        return maxIndex;
    }


    ///  SWAR skipping of spaces, linefeeds, and cr-lf combinations only.
    ///  to detect cr-lf pairs, we need to do some shifting. therefore
    ///  we read in data 8 bytes but advance 7 bytes each time.
    ///  a 0x00 if we match a blank, or 0x80 if there is a nonblank (as defined here)
    ///
    ///  *initial prototype* single iteration
    @SuppressWarnings( "unused" )
    private static long skipBlankLong(long in) {
        //System.out.println( "SWAR::skipBlank(long)");
        //System.out.println( "    INPUT:" + (toHex( in )) );
        long isAscii = HI_BITS & ~in;
        //System.out.println( "  isAscii:" + (toHex( isAscii )) );
        // all spots
        long eqLF = (in ^ LF_MASK) - LO_BITS;
        //System.out.println( "     eqLF:" + (toHex( eqLF )) );
        //

        // we match all bytes, but we only care about the first 7 bytes here
        //System.out.println( "    eqLF7:" + (toHex( eqLF7 )) );
        long eqSpace = (in ^ (SPC_MASK)) - LO_BITS;
        //System.out.println( "    eqSPC:" + (toHex( eqSpace7 )) );
        long eqCR = (in ^ (CR_MASK)) - LO_BITS;
        //System.out.println( "     eqCR:" + (toHex( eqCR7 )) );

        // now, detect CR-LF pairs
        long eqCRLF = (eqLF << 8) & eqCR;
        //System.out.println( "     eqCRLF:" + (toHex( eqCRLF )) );

        // result
        long result = (isAscii & (eqLF | eqSpace | eqCRLF)) ^ HI_BITS;
        //System.out.println( "     result:" + (toHex( result )) );
        // important!
        result = result & MASK_LAST_BYTE;  // mask off last byte
        return result;
    }
}
