package fluent.syntax.parser;


import org.jspecify.annotations.NullMarked;

/// Internal interface used by FTLStream to choose between Scalar or SIMD methods.
@NullMarked
sealed interface FTLStreamOps {

    FTLStreamOps SIMD = new FTLStreamOps.StreamOpsSIMD();
    FTLStreamOps SCALAR = new FTLStreamOps.StreamOpsScalar();


    int skipToEOL(final byte[] array, final int startPos);

    int skipBlank(final byte[] array, final int startPos);

    int skipBlankInline(final byte[] array, final int startPos);


    long skipBlankBlock(final byte[] array, final int startPos);

    int skipBlankBlockNLC(final byte[] array, final int startPos);

    int getIdentifierEnd(final byte[] array, final int startPos);


    final class StreamOpsScalar implements FTLStreamOps {


        @Override
        public int skipToEOL(byte[] array, int startPos) {
            return ScalarOps.skipToEOL( array, startPos );
        }

        @Override
        public int skipBlank(byte[] array, int startPos) {
            return ScalarOps.skipBlank( array, startPos );
        }

        @Override
        public int skipBlankInline(byte[] array, int startPos) {
            return ScalarOps.skipBlankInline( array, startPos );
        }

        @Override
        public long skipBlankBlock(byte[] array, int startPos) {
            return ScalarOps.skipBlankBlock( array, startPos );
        }

        @Override
        public int skipBlankBlockNLC(byte[] array, int startPos) {
            return ScalarOps.skipBlankBlockNLC( array, startPos );
        }

        @Override
        public int getIdentifierEnd(byte[] array, int startPos) {
            return ScalarOps.getIdentifierEnd( array, startPos );
        }


    }


    final class StreamOpsSIMD implements FTLStreamOps {


        @Override
        public int skipToEOL(byte[] array, int startPos) {
            return SIMDOps.nextLF( array, startPos );
        }

        @Override
        public int skipBlank(byte[] array, int startPos) {
            return SIMDOps.skipBlank( array, startPos );
        }

        @Override
        public int skipBlankInline(byte[] array, int startPos) {
            return SIMDOps.skipBlankInline( array, startPos );
        }

        @Override
        public long skipBlankBlock(byte[] array, int startPos) {
            return SIMDOps.skipBlankBlock( array, startPos );
        }

        @Override
        public int skipBlankBlockNLC(byte[] array, int startPos) {
            // same as line-counting version, but we just ignore the lines counted
            final long packed = SIMDOps.skipBlankBlock( array, startPos );
            return CommonOps.unpackPosition( packed );
        }

        @Override
        public int getIdentifierEnd(byte[] array, int startPos) {
            return SIMDOps.getIdentifierEnd( array, startPos );
        }

    }


}
