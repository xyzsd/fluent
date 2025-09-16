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
package fluent.bundle;

import fluent.function.FluentFunction;
import fluent.function.FluentFunctionException;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;

///
/// Interface for caching fluent functions.
///
/// The [#uncached()] method here can be used if no caching is desirable, or for performance baseline testing.
///
/// A simple LRU cache implementation is provided via [LRUFunctionCache], which is *not* designed to be shared between
/// [FluentBundle]s of different locales.
///
/// A cache can be shared between multiple [FluentBundle]s of different locales, if the cache is keyed appropriately.
@NullMarked
public interface FluentFunctionCache {

    // todo : probably this should be renamed to 'computeIfAbsent' since that is what it must do
    ///  Get the cached function. If the function is not in the cache, create it via the factory.
    <T extends FluentFunction> T getFunction(FluentFunctionFactory<T> factory, Locale locale, Options options) throws FluentFunctionException;




    ///  Create a nonfunctional cache 'NOP Cache'
    ///  (no caching occurs).
    static FluentFunctionCache uncached() {
        return new FluentFunctionCache() {
            @Override
            public <T extends FluentFunction> T getFunction(FluentFunctionFactory<T> factory, Locale locale, Options options) throws FluentFunctionException {
                return factory.create( locale, options );
            }
        };
    }


}
