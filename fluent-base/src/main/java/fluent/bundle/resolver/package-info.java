/*
 *
 *  Copyright (C) 2021-2025, xyzsd (Zach Del) 
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
/// Resolver package, which contains the components responsible for rendering Fluent AST nodes into messages.
///
/// Contents:
/// - Resolver: stateless set of static methods that evaluate patterns and expressions, expand placeables,
///   and resolve references (messages, terms, variables, and functions).
/// - Scope: per-format-call state holder. Provides access to the bundle, function registry/cache, locale, default
///   options, the converted argument map, and keeps track of visited nodes and collected errors.
/// - ResolutionException: sealed hierarchy describing resolution-time failures (cycles, unknown references,
///   limit violations). Typically converted to FluentError values while being collected in Scope.
///
/// Usage:
/// Clients do not typically need to interact directly with this package. Instead, clients interact through this
/// package through FluentBundle, which creates a Scope and then delegates to Resolver to evaluate messages and patterns.
package fluent.bundle.resolver;