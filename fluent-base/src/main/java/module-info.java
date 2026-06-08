/*
 *
 *  Copyright (C) 2025, xyzsd (Zach Del)
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

///  Fluent: An implementation of the Mozilla Fluent language localization framework.
module net.xyzsd.fluent {
    // static-transitive: required at compile time, optional and runtime
    requires static transitive org.jspecify;
    // These are currently automatic modules ... careful
    // This will result in a warning (could be suppressed with @SuppressWarnings("requires-automatic") )
    requires com.ibm.icu;
    //
    // We export most everything, since core functionality could be useful to users.
    exports fluent.bundle;
    exports fluent.bundle.resolver;
    exports fluent.syntax.ast;
    exports fluent.syntax.parser;
    exports fluent.types;
    // Everything in fluent.function.* and descendants should be exported.
    exports fluent.function;
    exports fluent.function.functions.list;
    exports fluent.function.functions.list.reducer;
    exports fluent.function.functions.misc;
    exports fluent.function.functions.numeric;
    exports fluent.function.functions.string;
    exports fluent.function.functions.temporal;
}