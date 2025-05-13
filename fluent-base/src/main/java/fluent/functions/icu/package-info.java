
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
 * Built-in Fluent Functions, based on ICU4J (ICU)
 * <p>
 * This set of functions (alternative to fluent.functions.cldr) uses the ICU4J library.
 * Currently, ICU is only used for plural selection. This package should be preferred by
 * projects already using (or intending to use) ICU.
 * <p>
 * Most functions in this package are verbatim copies of the functions in the CLDR-based package.
 * However, given the wealth of formatters and general usefulness of the ICU in addition to plural formatting,
 * it is expected that these functions may be tailored to use the unique features of ICU in the future.
 * </p>
 */
package fluent.functions.icu;
