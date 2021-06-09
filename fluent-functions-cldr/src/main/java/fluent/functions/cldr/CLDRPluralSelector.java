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

package fluent.functions.cldr;


import fluent.functions.FunctionResources;
import net.xyzsd.plurals.PluralRule;
import net.xyzsd.plurals.PluralRuleType;

import java.util.Locale;


/**
 * Bridge implementation of plural selection logic, specific to the CLDR library.
 */
public class CLDRPluralSelector implements FunctionResources {

    private final PluralRule cardinalRule;
    private final PluralRule ordinalRule;
    private final Locale locale;

    CLDRPluralSelector(Locale locale) {
        this.locale = locale;

        cardinalRule = PluralRule.create( locale, PluralRuleType.CARDINAL ).orElseThrow(
                () -> new IllegalArgumentException("Unknown locale: "+locale)
        );
        ordinalRule = PluralRule.create( locale, PluralRuleType.ORDINAL ).orElseThrow(
                () -> new IllegalArgumentException("Unknown locale: "+locale)
        );
    }


    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public String selectCardinal(Number n) {
        return numberToRule( cardinalRule, n );
    }

    public String selectOrdinal(Number n) {
        return numberToRule( ordinalRule, n );
    }


    private static String numberToRule(PluralRule rule, Number n) {
        return switch ( rule.select( n ) ) {
            case ZERO -> "zero";
            case ONE -> "one";
            case TWO -> "two";
            case FEW -> "few";
            case MANY -> "many";
            case OTHER -> "other";
        };
    }
}

