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

import fluent.syntax.parser.FTLPatternParser.TextElementTermination;
import jdk.incubator.vector.*;

/// SIMD parsing utilities.
///
/// This uses the (currently incubating) `jdk.incubator.vector` classes.
///
/// To enable this at runtime, an additional argument must be supplied to the JVM:
/// `--add-modules jdk.incubator.vector`
///
/// Several of the algorithms could likely benefit from further optimizations
final class SIMD {

    // much more powerful and much easier to implement than SWAR
    //
    // note that some of these methods can be optimized further
    //
    // Unlike SWAR, we don't need padding (due to indexInRange masking).
    //
    // NOTE: for CR-LF detection, we ignore the last lane in a vector, and iterate by species size minus 1.
    // this may impair cache/cache line performance. So we may need to think about an alternative scheme.
    // ALSO, consider using LUT instead of blend?


    // suitable for upto 64 lanes (8*64=512 bits). Used to generate masks.
    private static final long LONG_FF = 0xFF_FF_FF_FF_FF_FF_FF_FFL;

    // vector species we are using
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    // ASCII chars for comparison
    private static final Vector<Byte> LOWERCASE_A = SPECIES.broadcast( 0x61 );
    private static final Vector<Byte> LOWERCASE_Z = SPECIES.broadcast( 0x7a );
    private static final Vector<Byte> UPPERCASE_A = SPECIES.broadcast( 0x41 );
    private static final Vector<Byte> UPPERCASE_Z = SPECIES.broadcast( 0x5a );
    private static final Vector<Byte> NUM_0 = SPECIES.broadcast( 0x30 );
    private static final Vector<Byte> NUM_9 = SPECIES.broadcast( 0x39 );
    private static final Vector<Byte> UNDERSCORE = SPECIES.broadcast( 0x5f );
    private static final Vector<Byte> HYPHEN = SPECIES.broadcast( 0x2d );
    private static final Vector<Byte> SPACE = SPECIES.broadcast( 0x20 );
    private static final Vector<Byte> CR = SPECIES.broadcast( 0x0d );
    private static final Vector<Byte> LF = SPECIES.broadcast( 0x0a );
    private static final Vector<Byte> OPEN_BRACE = SPECIES.broadcast( 0x7b );
    private static final Vector<Byte> CLOSE_BRACE = SPECIES.broadcast( 0x7d );

    // these correspond to the enum ordinals in FTLPatternParser.TextSliceType
    private static final Vector<Byte> IDX_1_LF = SPECIES.broadcast( 0x01 );
    private static final Vector<Byte> IDX_2_CRLF = SPECIES.broadcast( 0x02 );
    private static final Vector<Byte> IDX_3_OPEN = SPECIES.broadcast( 0x03 );
    private static final Vector<Byte> IDX_4_CLOSE = SPECIES.broadcast( 0x04 );

    // all lanes set
    private static final VectorMask<Byte> ALL_LANES_MASK = VectorMask.fromLong( SPECIES, LONG_FF );
    // first lane unset; e.g., Mask[.TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT] (for 32 lanes)
    private static final VectorMask<Byte> IGNORE_FIRST_LANE_MASK = VectorMask.fromLong( SPECIES,
            (LONG_FF << 1) );
    // last lane unset; e.g., Mask[TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT.] (for 32 lanes)
    private static final VectorMask<Byte> IGNORE_LAST_LANE_MASK = leftShift( 1 );


    private SIMD() {}


    /// Extract an identifier.
    /// This extracts a Fluent-conformant identifier in the form `[A-Za-z]([A-Z][a-z][0-9][\-])*`
    ///
    /// Returns the first non-matching character that is nonconformant (usually indicating the end of the identifier).
    /// If the first non-matching character is equal to startIndex, then the identifier begins with an illegal character.
    static int getIdentifierEnd(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length;    // if there is padding at end, must subtract here
        // first vector
        {
            final VectorMask<Byte> rangeMask = SPECIES.indexInRange( startIndex, maxIndex );
            final ByteVector in = ByteVector.fromArray( SPECIES, buf, startIndex, rangeMask );
            // ignore first lane (only applies to digits and punctuation)
            // mask bits in 'firstUnset' indicate *valid* characters.
            // we want first UNSET bit (first invalid character).
            // So, we negate the mask then look for the first set bit.
            final int firstUnset = idUpperAndLower( in, IGNORE_FIRST_LANE_MASK ).not().firstTrue();
            // VLENGTH if all are true (and we iterate again)
            if (firstUnset != SPECIES.length()) {
                return firstUnset + startIndex;
            }
        }

        // continuation
        for (int i = (startIndex + SPECIES.length()); i < maxIndex; i += SPECIES.length()) {
            final VectorMask<Byte> rangeMask = SPECIES.indexInRange( i, maxIndex );
            final ByteVector in = ByteVector.fromArray( SPECIES, buf, i, rangeMask );

            // now we check all lanes and negate
            final int firstUnset = idUpperAndLower( in, ALL_LANES_MASK ).not().firstTrue();
            if (firstUnset != SPECIES.length()) {
                return firstUnset + i;
            }
        }
        return maxIndex;
    }


    // upper case, lower case, digits, and allowed punctuation (-_).
    // digits and punctuation can be masked off
    private static VectorMask<Byte> idUpperAndLower(final ByteVector in, final VectorMask<Byte> mask) {
        final VectorMask<Byte> isAlphabetic = in.compare( VectorOperators.UNSIGNED_GE, UPPERCASE_A )
                .and( in.compare( VectorOperators.UNSIGNED_LE, UPPERCASE_Z ) )
                .or(
                        in.compare( VectorOperators.UNSIGNED_GE, LOWERCASE_A )
                                .and( in.compare( VectorOperators.UNSIGNED_LE, LOWERCASE_Z ) )
                );


        // mask these as needed
        final VectorMask<Byte> isDigit = in.compare( VectorOperators.UNSIGNED_GE, NUM_0 )
                .and( in.compare( VectorOperators.UNSIGNED_LE, NUM_9 ) )
                .and( mask );

        final VectorMask<Byte> isPunctuation = in.compare( VectorOperators.EQ, HYPHEN )
                .or( in.compare( VectorOperators.EQ, UNDERSCORE ) )
                .and( mask );

        return isAlphabetic.or( isDigit ).or( isPunctuation );
    }


    ///  Find position of next LF in buffer, or return last position in buf.
    ///  Used for skipping comments
    static int nextLF(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length;    // if there is padding at end, must subtract here (e.g., final int maxIndex = buf.length - PAD;)
        for (int i = startIndex; i < maxIndex; i += SPECIES.length()) {
            final VectorMask<Byte> rangeMask = SPECIES.indexInRange( i, maxIndex );
            final ByteVector in = ByteVector.fromArray( SPECIES, buf, i, rangeMask );
            final VectorMask<Byte> isLF = in.compare( VectorOperators.EQ, LF );

            // at this point, isLF.firstTrue() is the first LF found. We could return that (unless none is found,
            // firstTrue() will return the vector lane length, and we iterate again).
            // HOWEVER, what if there is a contiguous run of LFs?
            //   00110101 > index 2, but would be nice to have it be index 3    [index is the lane index]
            //   mask.firstTrue() : first set bit (2)  call this 'firstLF'
            //   then shift LEFT by 2
            //   00110101 becomes
            //   11010100 (shifted left, 0-filled on right)
            //   now negate the mask
            //   00101011
            //   firstTrue() is first byte AFTER the last contiguous LF
            //   so if (firstTrue() < SPECIES.length) then return (firstTrue() - 1) + firstLF

            // NOTE: while the above is an interesting thought exercise, this may not be useful in practice.
            // this method if for skipping comments, and the extra effort may not be worthwhile.

            if (isLF.anyTrue()) {
                return isLF.firstTrue() + i;
            }
        }
        return maxIndex;
    }

    ///  Find the first non-space character (since line endings have LF or CRLF, this always stays on a single line)
    static int skipBlankInline(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length;    // if there is padding at end, must subtract here (e.g., final int maxIndex = buf.length - PAD;)
        for (int i = startIndex; i < maxIndex; i += SPECIES.length()) {
            final VectorMask<Byte> rangeMask = SPECIES.indexInRange( i, maxIndex );
            final ByteVector in = ByteVector.fromArray( SPECIES, buf, i, rangeMask );

            final VectorMask<Byte> found = in.compare( VectorOperators.NE, SPACE );

            if (found.anyTrue()) {
                return found.firstTrue() + i;
            }
        }
        return maxIndex;
    }


    ///  Find first non-blank (by Fluent definition of whitespace) character.
    static int skipBlank(final byte[] buf, final int startIndex) {
        return skipBlankRanged( buf, startIndex, buf.length );
    }

    /// True if the given range within buf[] only consists of Fluent whitespace
    /// (consists only of space, LF, or CR-LF pair)
    static boolean isBlank(final byte[] buf, final int startIndex, final int endIndex) {
        return (skipBlankRanged( buf, startIndex, endIndex ) == endIndex);
    }


    /// Skip blanks.
    /// This skips contiguous runs of ASCII Space (0x20), LF (0x0a), and CR-LF pairs (0x0d,0x0a).
    /// Implementation approaches (for CRLF detection)
    ///      (method 1) 2 windows read from array, offset by 1. Increment by SPECIES.LENGTH
    ///      (method 2) 1 read from array, and shifted left by one lane (NOT rotated)
    ///                 byteVector.slice(1). Thus we are only comparing SPECIES.LENGTH-1 bytes,
    ///                 and we must increment by SPECIES.LENGTH-1
    ///     (method 3) use slice to combine with next vector, like a carry-over. More complex I think.
    ///
    /// This is a ranged check; the endIndex is useful to implement both isBlank() and
    /// skipBlank()
    static int skipBlankRanged(final byte[] buf, final int startIndex, final int maxIndex) {
        final int increment = SPECIES.length() - 1;
        for (int i = startIndex; i < maxIndex; i += increment) {
            final VectorMask<Byte> rangeMask = SPECIES.indexInRange( i, maxIndex );
            final ByteVector in = ByteVector.fromArray( SPECIES, buf, i, rangeMask );

            // we match all lanes, BUT the last lane must be ignored -- we will mask it off later.
            final VectorMask<Byte> eqSpace = in.compare( VectorOperators.EQ, SPACE );
            final VectorMask<Byte> eqLF = in.compare( VectorOperators.EQ, LF );

            // we only care about CR if it is followed by LF.
            // so we shift the LF mask left by 1 lane, and & it with the CR mask.
            // of course we cannot evaluate the rightmost (highest) lane, which we ignore.
            // This is why we advance through the array by SPECIES.length() - 1 instead of SPECIES.length(),
            // and mask off the last (highest) lane for eqSpace and eqLF
            final VectorMask<Byte> eqCRLF = in.compare( VectorOperators.EQ, CR )
                    .and( leftShift( eqLF, 1 ) );

            // combine, and invert mask to get the first that does NOT match a run.
            final VectorMask<Byte> combined = eqSpace
                    .or( eqLF )
                    .or( eqCRLF )
                    .not()
                    .and( IGNORE_LAST_LANE_MASK );

            if (combined.anyTrue()) {
                return combined.firstTrue() + i;
            }
        }
        return maxIndex;
    }


    ///  SkipBlankBlock
    ///
    /// Basic Algorithm:
    /// - find first non-blank:
    ///     - Find space, LF, and CRs followed by LFs
    ///     - combine above
    ///     - invert mask; firstTrue in mask is first nonblank octet
    /// - if first nonblank is within the current vector:
    ///     - create a mask where all lanes from first-non-blank to end of vector are UNset
    ///     - combine that mask with LF mask
    ///     - first true lane (plus one), from startIndex, is our block start position
    /// - if first nonblank NOT in vector:
    ///     - find LAST lf (no masking), keep for next iteration
    ///
    /// in addition to the above, we keep track of the number of lines (by counting linefeeds)
    /// in testing this is proven to be equivalent to the scalar version.
    ///
    /// returns a *packed long* storing the position and line count
    ///
    static long skipBlankBlock(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length;    // if there is padding at end, must subtract here (e.g., final int maxIndex = buf.length - PAD;)
        final int increment = SPECIES.length();

        int lastLF = startIndex;
        int lineCount = 0;
        for (int i = startIndex; i < maxIndex; i += increment) {
            final ByteVector in = ByteVector.fromArray( SPECIES, buf, i, SPECIES.indexInRange( i, maxIndex ) );
            final ByteVector inShifted = ByteVector.fromArray( SPECIES, buf, i + 1, SPECIES.indexInRange( i + 1, maxIndex ) );

            // we are not ignoring the last lane, since we use 'inShifted' for the CR-LF comparison
            // our original approach shifted the LF mask, and we incremented by SPECIES.length() - 1
            final VectorMask<Byte> eqSpace = in.compare( VectorOperators.EQ, SPACE );
            final VectorMask<Byte> eqLF = in.compare( VectorOperators.EQ, LF );

            // find CR-LF pairs. This is true for all lanes that equal (CR & LF_shifted)
            final VectorMask<Byte> eqCRLF = in.compare( VectorOperators.EQ, CR )
                    .and( inShifted.compare( VectorOperators.EQ, LF ) );

            // inverted mask for 'blanks' (LF, CR (followed by LF), space); any set lanes here
            // indicate non-blanks
            final VectorMask<Byte> combinedNOT = eqSpace
                    .or( eqLF )
                    .or( eqCRLF )
                    .not();

            // we found a non-blank. time to terminate.
            if (combinedNOT.anyTrue()) {
                final int firstNonBlankLane = combinedNOT.firstTrue();

                // find the last LF lane (if any!) in this vector, that precedes the firstNonBlankLane
                final VectorMask<Byte> maskedLF = eqLF.and( maskLanesTo( firstNonBlankLane ) );
                if (maskedLF.anyTrue()) {
                    // position: the next character after the LF just prior to the first detected non-blank char (whew!)
                    // or, failing that, 'startPosition'.
                    final int position = (i + maskedLF.lastTrue() + 1);
                    // line count: number of linefeeds (also works for CRLF) (which is: maskedLF.trueCount())
                    return FTLStream.packLong( position, +maskedLF.trueCount() );
                } else {
                    // use the LF from a prior vector (or start if none), with total lineCount
                    return FTLStream.packLong( lastLF, lineCount );
                }
            }

            // iterate again. but we need to check for the last LF (unmasked), and store it
            if (eqLF.anyTrue()) {
                lastLF = eqLF.lastTrue() + i + 1;
                lineCount += eqLF.trueCount();
            }
        }
        return FTLStream.packLong( startIndex, lineCount );
    }


    ///  Customized matcher for FTLPatternParser.getTextSlice()
    ///  Returns a packed long consisting of position and ordinal
    static long nextTSChar(final byte[] buf, final int startIndex) {
        final int maxIndex = buf.length;    // if there is padding at end, must subtract here (e.g., final int maxIndex = buf.length - PAD;)
        final int increment = SPECIES.length() - 1;
        for (int i = startIndex; i < maxIndex; i += increment) {
            final VectorMask<Byte> rangeMask = SPECIES.indexInRange( i, maxIndex );
            final ByteVector in = ByteVector.fromArray( SPECIES, buf, i, rangeMask );

            // we match all lanes, BUT the last lane must be ignored -- we will mask it off later.
            final VectorMask<Byte> eqOpenBrace = in.compare( VectorOperators.EQ, OPEN_BRACE );
            final VectorMask<Byte> eqCloseBrace = in.compare( VectorOperators.EQ, CLOSE_BRACE );
            final VectorMask<Byte> eqLF = in.compare( VectorOperators.EQ, LF );

            // we only care about CR if it is followed by LF.
            // so we shift the LF mask left by 1 lane, and & it with the CR mask.
            // of course we cannot evaluate the rightmost (highest) lane, which we ignore.
            // This is why we advance through the array by SPECIES.length() - 1 instead of SPECIES.length(),
            // and mask off the last (highest) lane for eqSpace and eqLF
            final VectorMask<Byte> eqCRLF = in.compare( VectorOperators.EQ, CR )
                    .and( leftShift( eqLF, 1 ) );

            // indices assigned based on type
            final ByteVector blended = ByteVector.zero( SPECIES )
                    .blend( IDX_1_LF, eqLF )
                    .blend( IDX_2_CRLF, eqCRLF )
                    .blend( IDX_3_OPEN, eqOpenBrace )
                    .blend( IDX_4_CLOSE, eqCloseBrace )
                    .and( IGNORE_LAST_LANE_MASK.toVector() );

            // combined
            final VectorMask<Byte> combined = eqOpenBrace.or( eqCloseBrace ).or( eqLF ).or( eqCRLF )
                    .and( IGNORE_LAST_LANE_MASK );

            if (combined.anyTrue()) {
                final int firstTrue = combined.firstTrue();
                return FTLStream.packLong(
                        firstTrue + i,
                        blended.lane( firstTrue )
                );
            }
        }
        // position : max, EOF
        return FTLStream.packLong( maxIndex, TextElementTermination.EOF.ordinal() );
    }


    ///  Create a VectorMask with all lanes equal to or greater than the given lane, set.
    ///  All lower lanes are unset. So, maskLanesFrom(1) would leave lane 0 unset, and all lanes from
    ///  1 to SPECIES.length() would be set.
    private static VectorMask<Byte> maskLanesFrom(final int fromLane) {
        // if fromLane not in bounds ... could be exciting
        return VectorMask.fromLong( SPECIES, (LONG_FF << fromLane) );
    }

    ///  Create a VectorMask with all lanes from zero to the given lane (inclusive) set. All higher lanes are unset.
    ///  All lower lanes are unset. So, maskLanesFrom(1) would leave lane 0 set, and all lanes from
    ///  1 to SPECIES.length() would be unset.
    private static VectorMask<Byte> maskLanesTo(final int toLane) {
        return maskLanesFrom( toLane ).not(); // or could use
    }


    ///  Create a VectorMask with the (highest) n lanes unset, and all other lanes set
    private static VectorMask<Byte> leftShift(final int nLanes) {
        // 64 == length of long (as used in LONG_FF)
        return VectorMask.fromLong( SPECIES, LONG_FF >>> ((64 - SPECIES.length()) + nLanes) );
    }

    ///  Shift a VectorMask by n lanes, from higher lanes -> lower lanes. Lanes to the right are left unset.
    private static VectorMask<Byte> leftShift(final VectorMask<Byte> mask, final int nLanes) {
        // note that the vectors have 'little endian' order (lowest lanes on left, highest lanes on right)
        // but Java is big endian. So the shifts are reversed. Which can be confusing.
        return VectorMask.fromLong( SPECIES, mask.toLong() >>> nLanes );
    }


}
