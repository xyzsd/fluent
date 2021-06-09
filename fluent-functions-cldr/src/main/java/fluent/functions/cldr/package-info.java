
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

/**
 * Built-in Fluent Functions.
 * <p>
 * By convention, FluentFunction implementations should be postfixed by a "Fn", and the name should by similar to the function name.
 * For example, the NUMBER() function is in a class named "NumberFn".
 * </p>
 * <p>
 * Please refer to FluentFunction for additional information.
 * </p>
 */
/**
 * Built-in Fluent Functions, based on CLDR-plurals (CLDR)
 * <p>
 * This set of functions (alternative to fluent.functions.cldr uses the CLDR-plural-rules library,
 * which is a lightweight library which only handles plural selection (and hence its only dependency).
 * <p>
 * The functions in this package are based on the JDK and require no additional dependencies.
 * <p>
 */
package fluent.functions.cldr;
