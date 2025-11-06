package fluent.syntax.parser;

final class ScalarOps {


    private ScalarOps() {}


    static int skipToEOL(final byte[] array, final int startPos) {
        int pos = startPos;
        while ((pos < array.length) && (array[pos] != '\n')) {
            pos++;
        }
        return pos;
    }

    static int skipBlank(final byte[] array, final int startPos) {
        int pos = startPos;
        while (pos < array.length) {
            final byte b = array[pos];
            if (b == ' ' || b == '\n') {
                pos++;
            } else if (b == '\r' && (pos < (array.length - 1)) && (array[pos + 1] == (byte) '\n')) {
                pos += 2;
            } else {
                break;
            }
        }
        return pos;
    }

    static int skipBlankInline(final byte[] array, final int startPos) {
        int pos = startPos;
        while ((pos < array.length) && (array[pos] == ' ')) {
            pos++;
        }
        return pos;
    }


    public static long skipBlankBlock(final byte[] array, final int startPos) {
        int count = 0;
        int pos = startPos;
        while (pos < array.length) {
            final int start = pos;

            pos = skipBlankInline(array, pos);

            byte cb = array[pos];
            if (cb == '\n') {
                pos++;
            } else if (cb == '\r' && (pos < (array.length - 1)) && (array[pos + 1] == (byte) '\n')) {
                pos += 2;
            } else {
                pos = start;
                break;
            }
            count++;
        }

        return CommonOps.packLong( pos, count );
    }

    public static int skipBlankBlockNLC(final byte[] array, final int startPos) {
        int pos = startPos;
        while (pos < array.length) {
            final int start = pos;

            pos = skipBlankInline(array, pos);

            byte cb = array[pos];
            if (cb == '\n') {
                pos++;
            } else if (cb == '\r' && (pos < (array.length - 1)) && (array[pos + 1] == (byte) '\n')) {
                pos += 2;
            } else {
                return start;
            }
        }
        return pos;
    }


    static int getIdentifierEnd(final byte[] array, final int startPos) {
        if (!CommonOps.isASCIIAlphabetic( array[startPos] )) {
            return startPos;
        }

        for (int i = (startPos + 1); i < array.length; i++) {
            if (!CommonOps.isValidIDPart( array[i] )) {
                return i;
            }
        }
        return array.length;
    }


}
