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

///  [SWAR][https://en.wikipedia.org/wiki/SWAR] Methods and utilities
///
///  All SWAR operations here will use a long, with 8-bit lanes. (8 lanes (bytes) per long)
///
/// There are a number of methods marked *private* in this class which are potentially useful
/// but as yet currently unused.
///
///  Methods in this class are implementations of (or inspired by) ideas from the following:
///     - [Daniel Lemire (blog)][https://lemire.me/blog/?s=swar]
///     - [Wojciech Muła][http://0x80.pl/notesen-swar.html]
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
    private static final long MASK_LAST_BYTE = 0xFFFFFFFFFFFFFF00L;

    ///  View a byte array as a long. Easier and more performant way to fill a Long with 8 bytes.
    ///  NOTE: Be wary of endianness depending upon the byte buffer used
    private static final VarHandle LONGVIEW = MethodHandles.byteArrayViewVarHandle(
            long[].class, ByteOrder.BIG_ENDIAN );


    private SWAR() {}



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


    // not used. different implementation
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
    static long broadcast(final byte b) {
        return 0x101010101010101L * b;
    }

    // Not currently used

    ///  Nice multichar detection
    private static int nextAnyOf3(final byte delim0, final byte delim1, final byte delim2,
                                  final byte[] buf, final int startIndex) {
        final long dBroad0 = broadcast( delim0 );
        final long dBroad1 = broadcast( delim1 );
        final long dBroad2 = broadcast( delim2 );
        return nextAnyOf3( dBroad0, dBroad1, dBroad2, buf, startIndex );
    }

    // Not currently used

    ///  find any of the given 4 next delimiters
    private static int nextAnyOf4(final byte delim0, final byte delim1, final byte delim2, final byte delim3,
                                  final byte[] buf, final int startIndex) {
        final long dBroad0 = broadcast( delim0 );
        final long dBroad1 = broadcast( delim1 );
        final long dBroad2 = broadcast( delim2 );
        final long dBroad3 = broadcast( delim3 );
        return nextAnyOf4( dBroad0, dBroad1, dBroad2, dBroad3, buf, startIndex );
    }

    // Not currently used

    ///  find any of the given 3 next delimiters but use pre-broadcasted longs
    private static int nextAnyOf3(final long dBroad0, final long dBroad1, final long dBroad2,
                                  final byte[] buf, final int startIndex) {

        final int maxIndex = buf.length - PAD;

        for (int i = startIndex; i < maxIndex; i += 8) {
            // todo: verify bytecode that intermediates are removed (t0,t1)
            final long input = (long) LONGVIEW.get( buf, i );
            final long lo7bits = input & MASK_7F;
            final long d0 = (lo7bits ^ dBroad0) + MASK_7F;
            final long d1 = (lo7bits ^ dBroad1) + MASK_7F;
            final long d2 = (lo7bits ^ dBroad2) + MASK_7F;

            //final long t0 = (d0 & d1 & d2) | input;
            //final long t1 = t0 & HI_BITS;
            //final long t2 = t1 ^ HI_BITS;

            final long t = (((d0 & d1 & d2) | input) & HI_BITS) ^ HI_BITS;

            if (t != 0) {
                // leading/trailing zeros functions are intrinsics
                // returns 0-63 if found, 64 if not found (all zeros)
                // right shift by 3 to convert to byte position
                return (Long.numberOfLeadingZeros( t ) >> 3) + i;
            }
        }

        return maxIndex;
    }

    // Not currently used

    ///  find any of the given 4 next delimiters but use pre-broadcasted longs
    private static int nextAnyOf4(final long dBroad0, final long dBroad1, final long dBroad2, final long dBroad3,
                                  final byte[] buf, final int startIndex) {

        final int maxIndex = buf.length - PAD;

        for (int i = startIndex; i < maxIndex; i += 8) {
            // todo: verify bytecode that intermediates are removed (t0,t1)
            final long input = (long) LONGVIEW.get( buf, i );
            final long lo7bits = input & MASK_7F;
            final long d0 = (lo7bits ^ dBroad0) + MASK_7F;
            final long d1 = (lo7bits ^ dBroad1) + MASK_7F;
            final long d2 = (lo7bits ^ dBroad2) + MASK_7F;
            final long d3 = (lo7bits ^ dBroad3) + MASK_7F;

            final long t0 = (d0 & d1 & d2 & d3) | input;
            final long t1 = t0 & HI_BITS;
            final long t2 = t1 ^ HI_BITS;

            if (t2 != 0) {
                // leading/trailing zeros functions are intrinsics
                // returns 0-63 if found, 64 if not found (all zeros)
                // right shift by 3 to convert to byte position
                return (Long.numberOfLeadingZeros( t2 ) >> 3) + i;
            }
        }

        return maxIndex;
    }


    // Not currently used

    /// As used in jdk.incubator.vector documentation; 'round down'
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

    // Not currently used
    // also has a terrible name

    /// Sort of the opposite of loopBound
    private static int roundUpBy8B(final int size) {
        // More general form:
        // SPECIES_LENGTH = 8;
        // roundUP = (size + (SPECIES_LENGTH -1)) & ~(SPECIES_LENGTH -1)
        //
        return (size + 7) & (-8);
    }

    // Not currently used

    ///  find indices of set bits (LSB->MSB)
    // and if we want the index to be in the bitset or relative to the byte[] buffer
    // and should we return an int[] array, vs. use a lambda/callback
    // callback nicer
    private static void indexOfSetBits(final long bits) {
        long bitset = bits;
        while (bitset != 0) {
            long t = Long.lowestOneBit( bitset );
            int r = Long.numberOfTrailingZeros( bitset );
            // *execute something here*
            System.out.println(r);
            //
            bitset ^= t;
        }
    }

    // Not currently used

    ///  find indices of set bits
    ///  reverse-direction index (MSB->LSB)
    private static void indexOfSetBits2(final long bits) {
        long bitset = bits;
        while (bitset != 0) {
            long t = Long.highestOneBit( bitset );
            int r = Long.numberOfLeadingZeros( bitset );
            // *execute something here*
            System.out.println(r);
            //
            bitset ^= t;
        }
    }


    ///  for debugging
    private static String bin(byte in) {
        String s = Integer.toBinaryString( in );
        if (s.length() <= 8) {
            return "0".repeat( 8 - s.length() ) + s;
        }
        return s;
    }

    ///  For debugging
    private static void printBuf(byte[] buffer) {
        StringBuilder sb = new StringBuilder( 64 );
        for (int i = 0; i < buffer.length; i++) {
            char c = (char) (buffer[i] & 0xFF);
            switch (c) {
                case 0x0000 -> sb.append( "<NUL>" );
                case '\n' -> sb.append( "<LF>" );
                case '\r' -> sb.append( "<CR>" );
                default -> sb.append( c );
            }
        }
        System.out.println( sb );
    }

    ///  For debugging
    private static String b2char(byte b) {
        char c = (char) (b & 0xFF);
        return switch (c) {
            case 0x0000 -> ("<NUL>");
            case '\n' -> ("<LF>");
            case '\r' -> ("<CR>");
            default -> String.valueOf( c );
        };
    }

    ///  For debugging
    private static String toHex(long l) {
        //System.out.println(Long.toBinaryString( l ));
        final int radix = 16;
        String s = Long.toUnsignedString( l, radix );
        int pad = radix - s.length();
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
    ///  *initial prototype*
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
