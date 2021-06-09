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

import fluent.functions.FluentImplicit;
import fluent.functions.ImplicitFormatter;
import fluent.functions.ResolvedParameters;
import fluent.syntax.AST.SelectExpression;
import fluent.syntax.AST.Variant;
import fluent.bundle.resolver.Scope;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;


/**
 * FluentNumber wraps a numeric type.
 * <p>
 *     All numeric types are converted into Long, Double, or BigDecimal as appropriate.
 * </p>
 * @param <T>
 */
public /*sealed*/ interface FluentNumber<T extends Number> extends FluentValue<T> {


    /**
     * Create a FluentNumber for a given Number type. Returns the best fitting FluentNumber type.
     *
     * @param n Number type to convert to a FluentNumber
     * @return FluentNumber best representing the given Number type
     * @throws IllegalArgumentException for unknown Number types
     * @throws NullPointerException if Number is null
     */
    static FluentNumber<?> from(@NotNull final Number n) {
        Objects.requireNonNull(n);

        if (n instanceof Integer || n instanceof Long) {
            return new FluentLong( n.longValue() );
        } else if (n instanceof Double || n instanceof Float) {
            return new FluentDouble( n.doubleValue() );
        } else if (n instanceof BigDecimal bigDecimal) {
            return new FluentBigDecimal( bigDecimal );
        } else if (n instanceof BigInteger bigInteger) {
            return new FluentBigDecimal( new BigDecimal( bigInteger ) );
        } else if(n instanceof Short || n instanceof Byte) {
            // separate branch; shorts & bytes are expected to be used less frequently
            return new FluentLong( n.longValue() );
        }

        throw new IllegalArgumentException( String.valueOf( n ) );
    }

    /** Create a FluentBigDecimal from a BigDecimal */
    static FluentBigDecimal of(@NotNull BigDecimal bigDecimal) {
        return new FluentBigDecimal( bigDecimal );
    }

    /** Create a FluentBigDecimal from a BigInteger */
    static FluentBigDecimal of(@NotNull BigInteger bigInteger) {
        return new FluentBigDecimal( new BigDecimal( bigInteger ) );
    }

    /** Create a FluentLong from a long */
    static FluentLong of(long value) {
        return new FluentLong( value );
    }

    /** Create a FluentDouble from a double */
    static FluentDouble of(double value) {
        return new FluentDouble( value );
    }


    /**
     * Format the number. This is equivalent to calling NUMBER() on this FluentValue without any additional arguments.
     */
    @Override
    default String format(final Scope scope) {
        // 'NUMBER()' function called, with 'default' arguments, to apply default localized formatter
        return ((ImplicitFormatter) scope.bundle().implicit( FluentImplicit.Implicit.NUMBER ))
                .format( this, scope );
    }

    /**
     * By default, for select statements, attempt to match a Variant that corresponds to the
     * cardinal plural form of this number. If there is no match, the default Variant is returned.
     */
    @Override
    default Variant select(final SelectExpression selectExpression, final ResolvedParameters params, final Scope scope) {
        final String categoryName = scope.fnResources.selectCardinal( value() );
        return selectExpression.matchOrDefault( categoryName );
    }

    /**
     * Type as BigDecimal
     */
    @NotNull BigDecimal asBigDecimal();



    // Concrete types ---------------------------------------------------


    record FluentLong(@NotNull Long value) implements FluentNumber<Long> {

        @Override
        @NotNull
        public BigDecimal asBigDecimal() {
            return BigDecimal.valueOf( value );
        }
    }

    record FluentDouble(@NotNull Double value) implements FluentNumber<Double> {
        @Override
        @NotNull
        public BigDecimal asBigDecimal() {
            return BigDecimal.valueOf( value );
        }
    }

    record FluentBigDecimal(@NotNull BigDecimal value) implements FluentNumber<BigDecimal> {
        @Override
        @NotNull
        public BigDecimal asBigDecimal() {
            return value;
        }
    }

}
