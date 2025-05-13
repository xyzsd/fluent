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

import fluent.functions.ResolvedParameters;
import fluent.syntax.AST.SelectExpression;
import fluent.syntax.AST.Variant;
import fluent.bundle.resolver.Scope;
import org.jspecify.annotations.NullMarked;


/**
 * Interface for all FluentValues.
 * <p>
 *     Supplied arguments for Fluent messages use FluentValues. FluentValues are created from standard Java objects
 *     (String, numeric types, etc.) and used internally when processing Fluent messages.
 *
 *     FluentValues also control formatting of the values during message creation.
 * </p>
 *
 *
 * @param <T> type
 */
@NullMarked
public sealed interface FluentValue<T>
        permits FluentCustom, FluentNumber, FluentString, FluentTemporal, FluentError {


    T value();

    /**
     * This value, formatted as a String.
     * @param scope Scope
     * @return formatted String. Never null.
     */
    String format(Scope scope);


    /**
     * Handle fluent 'select'
     * <p>
     *     By default, this method picks the default variant. No attempt to match variants is performed.
     * </p>
     *
     *
     * @param selectExpression SelectExpression
     * @param params ResolvedParameters
     * @param scope Scope
     * @return
     */
    default Variant select(final SelectExpression selectExpression, final ResolvedParameters params, final Scope scope) {
        return selectExpression.defaultVariant();
    }
}
