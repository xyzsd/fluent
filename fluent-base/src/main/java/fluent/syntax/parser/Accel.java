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

/// Internal interface used to decide which accelerated functions to use
sealed interface Accel {

    // This interface is used to call the SWAR or SIMD static implementations.
    //
    // An alternative approach would be to use a class with static MethodHandles,
    // defined in a static initializer block, with wrappers calling invokeExact().
    // MethodHandles could be more performant (easier inlining?) but also require having 'throws Throwable'
    // bubbled up or wrapped. The comparison has not yet been explored. JVM traces would have to be
    // reviewed.
    //
    // Another potential option: Lazy Constants (ex-StableValue)
    //


    /// Get the accelerator type depending upon the JDK and execution environment.
    /// The returned implementation can be used like a static.
    static Accel get() {
        try {
            // this will fail if either:
            //  (a) there is no jdk.incubator.vector.ByteVector
            //  (b) runtime VM option not set: "--add-modules jdk.incubator.vector"
            Class.forName( "jdk.incubator.vector.ByteVector" );
            return new AccelSIMD();
        } catch (ClassNotFoundException e) {
            return new AccelSWAR();
        }
    }

    int nextLF(final byte[] buf, final int startIndex);

    int getIdentifierEnd(final byte[] buf, final int startIndex);

    int skipBlank(final byte[] buf, final int startIndex);

    int skipBlankInline(final byte[] buf, final int startIndex);

    boolean isBlank(final byte[] buf, final int startIndex, final int endIndex);

    ///  required padding
    int pad();

    ///  true if vectorized via SIMD
    boolean isVector();


    // functions like a static, but isn't (and may not have as many optimization opportunities)
    // this assumes (and requires!) that there is no class state here or in SWAR
    final class AccelSWAR implements Accel {

        private AccelSWAR() {}

        @Override
        public boolean isVector() {
            return false;
        }

        @Override
        public int pad() {
            return SWAR.PAD;
        }

        @Override
        public int getIdentifierEnd(byte[] buf, int startIndex) {
            return SWAR.getIdentifierEnd( buf, startIndex );
        }

        @Override
        public int nextLF(byte[] buf, int startIndex) {
            return SWAR.nextLF( buf, startIndex );
        }

        @Override
        public int skipBlankInline(byte[] buf, int startIndex) {
            return SWAR.skipBlankInline( buf, startIndex );
        }

        @Override
        public boolean isBlank(byte[] buf, int startIndex, int endIndex) {
            return SWAR.isBlank( buf, startIndex, endIndex );
        }

        @Override
        public int skipBlank(byte[] buf, int startIndex) {
            return SWAR.skipBlank( buf, startIndex );
        }

        @Override
        public String toString() {
            return "SWAR";
        }
    }

    // functions like a static, but isn't (and may not have as many optimization opportunities)
    // this assumes (and requires!) that there is no class state here or in SIMD
    final class AccelSIMD implements Accel {

        private AccelSIMD() {}

        @Override
        public boolean isVector() {
            return true;
        }

        @Override
        public int pad() {
            return 0;
        }

        @Override
        public int getIdentifierEnd(byte[] buf, int startIndex) {
            return SIMD.getIdentifierEnd( buf, startIndex );
        }

        @Override
        public int nextLF(byte[] buf, int startIndex) {
            return SIMD.nextLF( buf, startIndex );
        }

        @Override
        public int skipBlankInline(byte[] buf, int startIndex) {
            return SIMD.skipBlankInline( buf, startIndex );
        }

        @Override
        public int skipBlank(byte[] buf, int startIndex) {
            return SIMD.skipBlank( buf, startIndex );
        }

        @Override
        public boolean isBlank(byte[] buf, int startIndex, int endIndex) {
            return SIMD.isBlank( buf, startIndex, endIndex );
        }

        @Override
        public String toString() {
            return "SIMD";
        }
    }


}
