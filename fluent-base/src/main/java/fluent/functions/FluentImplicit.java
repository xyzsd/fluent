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

package fluent.functions;

/**
 * Implicit Fluent Functions
 * <p>
 *     An implicit function is a function that is called for types
 *     when a function name is not explicitly given.
 * </p>
 * <p>
 *     For example, if {@code $num} is a number (e.g., 7.0),
 *     the following placeables are equivalent: {@code {$num} } and {@code {NUMBER($num)} }
 * </p>
 * <p>
 *     Currently, there are only 3 implicit functions: JOIN(), NUMBER(), and TEMPORAL()
 * </p>
 * <p>
 *     Note to implementors:
 *     <ul>
 *         <li>JOIN() must also implement ImplicitReducer</li>
 *         <li>NUMBER(), TEMPORAL() must also implement ImplicitFormatter</li>
 *      </ul>
 * <p>
 *     All Implicit functions must be defined by a FluentFunctionFactory.
 * </p>
 */
public interface FluentImplicit extends FluentFunction {

    /**
     * The required Implicit Functions.
     */
    enum Implicit {
        NUMBER, TEMPORAL, JOIN
    }


    /**
     * Implementations should NOT override this, unlike standard FluentFunctions.
     * @return name of implicit
     */
    @Override
    default String name() {
        return id().name();
    }


    /** Identifier of this Implicit */
    Implicit id();


}
