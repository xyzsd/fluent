package fluent.syntax.parser;


final class CommonOps {
    // magic constants for scalar ASCII comparisons.
    private static final byte MAGIC_CAPS_ALPHA_OFFSET = ((byte) (-65 + Byte.MIN_VALUE)); // 'A' (uppercase A)
    private static final byte MAGIC_LC_ALPHA_OFFSET = ((byte) (-97 + Byte.MIN_VALUE)); // 'a' (lowercase A)
    private static final byte MAGIC_ALPHA_RANGE = ((byte) (26 + Byte.MIN_VALUE));  // 26 letters
    private static final byte MAGIC_HEX_LETTER_RANGE = ((byte) (6 + Byte.MIN_VALUE));  // 6 letter range
    private static final byte MAGIC_DIGIT_RANGE = ((byte) (10 + Byte.MIN_VALUE));    // 10 digit range
    private static final byte MAGIC_DIGIT_OFFSET = ((byte) (-48 + Byte.MIN_VALUE));    // '0' ascii value

    // internal
    static final int RADIX = 16;
    // EOF (0xFF) is not a valid Unicode character, and should
    // not be present in a stream unless malformed.
    static final byte EOF = (byte) 0xFF;

    private CommonOps() {}

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
    @SuppressWarnings("unused")
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

    /// Calculate the 1-based line number for an explicit position in the stream.
    ///
    /// If the position is invalid (e.g., EOF), 0 is returned.
    ///
    /// @param position a byte offset into the decoded stream
    /// @return the line number (>= 0); 0 indicates an invalid position (typically EOF)
    static int positionToLine(final byte[] array, final int position) {
        if (position < 0 || position >= array.length) {
            return 0;
        }

        // simple line count
        int lfCount = 1;        // linefeed count
        for (int i = 0; i < position; i++) {
            if (array[i] == '\n') {
                lfCount++;
            }
        }
        return lfCount;
    }



    ///  Given a packed long, return the high integer (used for file position)
    static int unpackPosition(final long packedLong) {
        return (int) (packedLong >> 32);   // Hi int
    }

    ///  Given a packed long, return the low integer
    private static int packedLow(final long packedLong) {
        return (int) (packedLong & 0xFF_FF_FF_FFL);   // low int
    }

    ///  Given a packed long, return the low integer (used for the enum ordinal index in textslice)
    static int unpackOrdinal(final long packedLong) {
        return packedLow( packedLong );
    }

    ///  Given a packed long, return the low integer (used for the line count)
    static int unpackLineCount(final long packedLong) {
        return packedLow( packedLong );
    }

    ///  Pack the given integers into a long.
    static long packLong(final int positionHI, final int otherLOW) {
        return ((long) positionHI << 32) | (otherLOW & 0xFF_FF_FF_FFL);
    }


}
