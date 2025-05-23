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

package fluent.types;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;


/// FluentNumber wraps a numeric type.
///
///     All numeric types are converted into Long, Double, or BigDecimal as appropriate.
///
/// @param <T>
@NullMarked
public sealed interface FluentNumber<T extends Number> extends FluentValue<T> {

    /// Create a FluentNumber for a given Number type. Returns the best fitting FluentNumber type.
    ///
    /// @param n Number type to convert to a FluentNumber
    /// @return FluentNumber best representing the given Number type
    /// @throws IllegalArgumentException for unknown Number types
    /// @throws NullPointerException if Number is null
    static FluentNumber<?> from(final Number n) {
        Objects.requireNonNull(n);
        return switch(n) {
            case Integer _, Long _, Short _, Byte _ -> new FluentLong(n.longValue());
            case Double _, Float _ -> new FluentDouble(n.doubleValue());
            case BigDecimal bigDecimal -> new FluentBigDecimal(bigDecimal);
            case BigInteger bigInteger -> new FluentBigDecimal( new BigDecimal( bigInteger ) );
            default ->  throw new IllegalArgumentException( String.valueOf( n ) );
        };
    }

    /// Create a FluentBigDecimal from a BigDecimal
    static FluentBigDecimal of(BigDecimal bigDecimal) {
        return new FluentBigDecimal( bigDecimal );
    }

    /// Create a FluentBigDecimal from a BigInteger
    static FluentBigDecimal of(BigInteger bigInteger) {
        return new FluentBigDecimal( new BigDecimal( bigInteger ) );
    }

    /// Create a FluentLong from a long or narrower integral type
    static FluentLong of(long value) {
        return new FluentLong( value );
    }

    /// Create a FluentDouble from a double or narrower type (float)
    static FluentDouble of(double value) {
        return new FluentDouble( value );
    }






    /// Type as BigDecimal
    BigDecimal asBigDecimal();



    // Concrete types ---------------------------------------------------


    record FluentLong(Long value) implements FluentNumber<Long> {

        @Override
        public BigDecimal asBigDecimal() {
            return BigDecimal.valueOf( value );
        }
    }

    record FluentDouble(Double value) implements FluentNumber<Double> {
        @Override
        public BigDecimal asBigDecimal() {
            return BigDecimal.valueOf( value );
        }
    }

    record FluentBigDecimal(BigDecimal value) implements FluentNumber<BigDecimal> {
        @Override
        public BigDecimal asBigDecimal() {
            return value;
        }
    }

}
