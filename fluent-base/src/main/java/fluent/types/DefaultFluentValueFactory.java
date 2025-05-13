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

import org.jetbrains.annotations.NotNull;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Default mappings:
 * <ul>
 *     <li>String values -> FluentString</li>
 *     <li>
 *         Numbers -> FluentNumbers:
 *         <ul>
 *             <li>Integral types (byte, short, int, long) -> FluentLong</li>
 *             <li>Decimal types (float, double) -> FluentDouble</li>
 *             <li>BigInteger, BigDecimal -> FluentBigDecimal</li>
 *             <li>Unknown number types -> IllegalArgumentException</li>
 *         </ul>
 *     </li>
 *     <li>TemporalAccessor -> FluentTemporal (note: java.util.Date is not supported)</li>
 *     <li>Any other object -> FluentCustom</li>
 * </ul>
 */
public class DefaultFluentValueFactory implements FluentValueFactory {

    private static final DefaultFluentValueFactory INSTANCE = new DefaultFluentValueFactory();


    public static FluentValueFactory create() {
        return INSTANCE;
    }


    /**
     * Create a FluentValue from a given Object
     * <p>
     *     Nulls are not permitted.
     * </p>
     * <p>
     *      If the value is already a FluentValue, no change occurs.
     * </p>
     * @param any non-null input
     * @param <T> type
     * @return
     */
    @Override
    public <T> FluentValue<?> toFluentValue(@NotNull final T any) {
        Objects.requireNonNull( any );
        if(any instanceof Collection<?> col) {
            throw new IllegalArgumentException("collection : "+col);
        }
        if (any instanceof FluentValue<?> v) {
            return v;   // prevent wrapping of a FluentValue into a FluentCustom
        } else if (any instanceof String s) {
            return FluentString.of( s );
        } else if (any instanceof Number n) {
            return FluentNumber.from( n );
        } else if (any instanceof TemporalAccessor t) {
            return new FluentTemporal( t );
        } else {
            return new FluentCustom<>( any );
        }
    }


    /**
     * Single item or supported collection (Set, List) becomes a List
     * <p>
     *     Nested collections are not supported.
     * </p>
     */
    @Override
    public <T> List<FluentValue<?>> toCollection(@NotNull final T in) {
        if(in instanceof Collection<?> collection) {
            if (in instanceof List || in instanceof Set) {
                return collection.stream()
                        .peek( item -> DefaultFluentValueFactory.checkNested(item, in) )
                        .<FluentValue<?>>map( this::toFluentValueNullsafe )
                        .toList();
            }
            throw new IllegalArgumentException("Unsupported collection type: "+in);
        } else {
            return List.of(toFluentValueNullsafe( in ));
        }
    }

    private static void checkNested(Object obj, Object in) {
        if(obj instanceof Collection<?>) {
            throw new IllegalArgumentException("Invalid arguments. Cannot have a collection nested within another collection. "+in);
        }
    }



    private DefaultFluentValueFactory() {}

}
