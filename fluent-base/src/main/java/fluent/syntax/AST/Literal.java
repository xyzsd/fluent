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

package fluent.syntax.AST;

import fluent.bundle.resolver.Scope;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;


@NullMarked
public /*sealed*/ interface Literal<T> extends InlineExpression {

    T value();


    record StringLiteral(String value) implements Literal<String> {

        /** Create the StringLiteral */
        public static StringLiteral of(final String s) {
            return new StringLiteral( s );
        }

        @Override
        public boolean needsIsolation() {
            return false;
        }

        @Override
        public List<FluentValue<?>> resolve(Scope scope) {
            return List.of( new FluentString( value ) );
        }
    }


    /*sealed*/ interface NumberLiteral<N extends Number> extends Literal<Number>, VariantKey
            /*permits LongLiteral, DoubleLiteral*/ {


        /**
         * Create a NumberLiteral from the given String
         *
         * @param s input text
         * @return a LongLiteral or DoubleLiteral as appropriate
         * @throws NumberFormatException if there is a parse exception
         */
        static NumberLiteral<?> from(final String s) throws NumberFormatException {
            if (s.indexOf( '.' ) > 0) {
                return new DoubleLiteral( Double.valueOf( s ) );
            } else {
                return new LongLiteral( Long.valueOf( s ) );
            }
        }

        @Override
        default List<FluentValue<?>> resolve(Scope scope) {
            return List.of( FluentNumber.from( value() ) );
        }

        @Override
        default String key() {
            return String.valueOf( value() );
        }
    }


    // todo: these were present for serialization issues ... but may be removed in the future

    final record LongLiteral(Long value) implements NumberLiteral<Long> {}


    final record DoubleLiteral(Double value) implements NumberLiteral<Double> {}
}
