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

package fluent.functions.icu.string;

import fluent.functions.FluentFunction;
import fluent.functions.ResolvedParameters;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentString;
import fluent.types.FluentValue;

import java.util.List;
import java.util.Locale;

/**
 *  CASE() : Change the case of a String value
 *  <p>
 *      Change the case of a String value. By default, changes case to UPPER case. Case conversion is
 *      locale-aware.
 *  </p>
 *  <p>
 *      Non-String values are passed through.
 *  </p>
 *  <p>
 * Options:
 *     <ul>
 *          <li>{@code style:} either {@code "upper"} (the default) or {@code "lower"}
 *     </ul>
 *  <p>
 *      Examples:
 *      <ul>
 *          <li>CASE("STRingVAlue") => "STRINGVALUE" </li>
 *          <li>CASE("STRingVAlue", style:"upper") => "STRINGVALUE" </li>
 *          <li>CASE("STRingVAlue", style:"lower") => "stringvalue" </li>
 *          <li>CASE(-5) => -5</li>
 *   </ul>
 */
public class CaseFn implements FluentFunction {

    public static final String NAME = "CASE";

    public CaseFn() {}

    private enum Style {
        UPPER, LOWER
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters params, final Scope scope) {
        FluentFunction.ensureInput( params );

        final Style style = params.options().asEnum(Style.class,  "style" )
                .orElse( Style.UPPER );
        final Locale locale = scope.bundle().locale();

        return params.valuesAll()
                .<FluentValue<?>>map( fv -> changeCase( fv, style, locale ) )
                .toList();
    }


    private static FluentValue<?> changeCase(FluentValue<?> in, Style style, Locale locale) {
        if(in instanceof FluentString fluentString) {
            return switch(style) {
                case UPPER -> FluentString.of( fluentString.value().toUpperCase( locale ) );
                case LOWER -> FluentString.of( fluentString.value().toLowerCase( locale ) );
            };
        } else {
            return in;
        }
    }
}
