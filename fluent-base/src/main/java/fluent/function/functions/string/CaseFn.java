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
package fluent.function.functions.string;

import fluent.function.*;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;
import java.util.function.Function;

/// ## CASE()
/// Change the case of a String pattern.
///
/// By default, changes case to UPPER case. Case conversion is locale-aware.
///
/// Non-String values are passed through.
///
/// Options:
/// - `style:` either `"upper"` (the default) or `"lower"`
///
/// ## Examples
/// {@snippet :
///     CASE("STRingVAlue")                 // "STRINGVALUE"
///     CASE("STRingVAlue", style:"upper")  // "STRINGVALUE"
///     CASE("STRingVAlue", style:"lower")  // "stringvalue"
///     CASE(-5)                            // -5   (as a FluentLong; non-string values are passed through)
/// }
/// 
///
@NullMarked
public enum CaseFn implements FluentFunctionFactory<FluentFunction.Transform>  {

    CASE;


    @Override
    public FluentFunction.Transform create(final Locale locale, final Options options) {
        // locale AND options sensitive
        final CaseFn.Style style = options.asEnum( CaseFn.Style.class,  "style" )
                .orElse( CaseFn.Style.UPPER );

        final Function<String,String> fn = switch(style) {
            case UPPER -> (String s) -> s.toUpperCase(locale);
            case LOWER -> (String s) -> s.toLowerCase(locale);
        };

        return FluentFunction.passthroughTransform(String.class, fn );
    }

    @Override
    public boolean canCache() {
        return true;    // though pattern is low
    }

    // for ease of option parsing
    private enum Style {
        UPPER, LOWER
    }


}
