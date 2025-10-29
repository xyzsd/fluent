package fluent.syntax.parser;

import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

@NullMarked
final class FTLStreamSWAR extends FTLStream {

    /// amount the buffer is padded (8 bytes typical)
    /// if we start on LAST byte, and a long is 8 bytes, we need 7 more bytes.
    /// however if we have a +1 offset for (say) CR-LF detection, we need 8 more bytes.
    /// This unfortunately forces us to copy the input byte buffer to add padding,
    /// Though block copies are pretty fast and we are not working with particularly large
    /// byte buffers in the scheme of things.
    ///
    /// An alternate approach would be to use the scalar code to finish the remaining bytes
    /// and use loop only until loopBound() (see method docs)
    private static final int PAD = 8;

    /// Hi bits. Also useful for determining if a byte (UTF-8) is ASCII or not.
    /// ASCII bytes will not have the high bit set.
    private static final long HI_BITS = 0x80_80_80_80_80_80_80_80L;
    ///  Low bits mask ('one mask')
    private static final long LO_BITS = 0x01_01_01_01_01_01_01_01L;
    ///  7F mask (7 bits)
    private static final long MASK_7F = 0x7F_7F_7F_7F_7F_7F_7F_7FL;
    ///  LF (linefeed)
    private static final long LF_MASK = 0x0a_0a_0a_0a_0a_0a_0a_0aL;
    ///  CR (carriage return)
    private static final long CR_MASK = 0x0d_0d_0d_0d_0d_0d_0d_0dL;
    ///  Space (ASCII space)
    private static final long SPACE_MASK = 0x20_20_20_20_20_20_20_20L;
    ///  Mask off final byte (ignore 8th byte)
    private static final long MASK_LAST_BYTE = 0xFF_FF_FF_FF_FF_FF_FF_00L;
    ///  All bits set
    private static final long ALL_BITS = 0xFF_FF_FF_FF_FF_FF_FF_FFL;

    ///  View a byte array as a long. Easier and more performant way to fill a Long with 8 bytes.
    ///  NOTE: Be wary of endianness depending upon the byte buffer used
    private static final VarHandle LONGVIEW = MethodHandles.byteArrayViewVarHandle(
            long[].class, ByteOrder.BIG_ENDIAN );



    FTLStreamSWAR(final byte[] array) {
        // awaiting JEP 513... out of preview in JDK 25
        super( copyAndPad( array ), array.length );
    }


    private static byte[] copyAndPad(final byte[] in) {
        // SWAR (padding required)
        final byte[] buf = new byte[in.length + PAD];
        for (int i = in.length; i < buf.length; i++) {
            buf[i] = EOF;
        }
        System.arraycopy( in, 0, buf, 0, in.length );
        return buf;
    }


    /// skip to the end of line (e.g., for skipping comments).
    /// This will skip to the end of a newline, ignoring a preceding '\r' if present.
    @Override
    void skipToEOL() {
        pos = nextLF_SWAR( seq, pos );
    }

    @Override
    void skipBlank() {
        position( skipBlank_SWAR( seq, pos ) );
    }

    @Override
    int skipBlankInline() {
        final int newPos = skipBlankInline_SWAR( seq, pos );
        final int start = pos;
        pos = newPos;
        return (newPos - start);
    }


    /// Find position of next LF in buffer, or last position in buf.
    /// ASSUMES BUFFER IS PADDED
    private static int nextLF_SWAR(final byte[] buf, final int startIndex) {
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

    ///  Find the first non-space (ASCII whitespace, 0x20)
    /// ASSUMES BUFFER IS PADDED
    private static int skipBlankInline_SWAR(final byte[] buf, final int startIndex) {
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
            final long t1 = ((((input & MASK_7F) ^ SPACE_MASK) + MASK_7F) | input) & HI_BITS;

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
    ///
    /// ASSUMES BUFFER IS PADDED
    private static int skipBlank_SWAR(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length - PAD;
        // NOTE: increment is 7 here! important!
        // basically an 8 byte window with 1 byte overlap
        for (int i = startIndex; i < maxIndex; i += 7) {
            final long in = (long) LONGVIEW.get( buf, i );

            long isAscii = HI_BITS & ~in;
            long eqLF = (in ^ LF_MASK) - LO_BITS;
            // we match all bytes, but we only care about the first 7 bytes here
            long eqSpace = (in ^ (SPACE_MASK)) - LO_BITS;
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

    ///  create a mask (long) from a byte
    @SuppressWarnings( "unused" )
    private static long broadcast(final byte b) {
        return 0x101010101010101L * b;
    }
}
