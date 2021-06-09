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

import java.util.Locale;
import java.util.Set;

/**
 * FluentFunctionFactory interface
 * <p>
 *     The FluentFunctionFactory is used to initialize functions within a Fluent bundle.
 *     Bundles require a FluentFunctionFactory when built.
 * </p>
 * <p>
 *     Currently, there are two implementations; one based on ICU and another based on the CLDR library.
 *     The the ICU- and CLDR-based function implementations are separated into separate modules; only one
 *     of the two modules is required.
 * </p>
 *
 */
public interface FluentFunctionFactory {

    /**
     * Initialize 'implicit' functions. These are functions that are required. See FluentImplicit for more information.
     */
    Set<FluentImplicit> implicits();

    /**
     * The set of optional, non-implicit functions. An empty set is permissible.
     */
    Set<FluentFunction> functions();

    /**
     * Per-bundle resources that are locale-dependent.
     */
    FunctionResources resources(Locale locale);

}
