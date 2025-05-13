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

package fluent.functions.icu;

import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.PluralRules;
import fluent.functions.FunctionResources;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Bridge implementation of plural selection logic, specific to ICU.
 */
public class ICUPluralSelector  implements FunctionResources {

    private final PluralRules cardinalRule;
    private final PluralRules ordinalRule;
    private final LocalizedNumberFormatter lnf;
    private final Locale locale;

    ICUPluralSelector(Locale locale) {
        this.locale = locale;
        cardinalRule = PluralRules.forLocale( locale, PluralRules.PluralType.CARDINAL );
        ordinalRule = PluralRules.forLocale( locale, PluralRules.PluralType.ORDINAL );
        // options to change trailing zero display does not seem to do anything.
        // formatter seems to remove trailing zeros UNLESS minimum/fixed fraction
        // size specified.
        lnf = NumberFormatter.withLocale( locale );
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public String selectCardinal(final Number num) {
        return cardinalRule.select( toFormattedNumber(num) );

    }

    public String selectOrdinal(final Number num) {
        return ordinalRule.select( toFormattedNumber(num) );
    }



    private FormattedNumber toFormattedNumber(final Number num) {
        if(num instanceof Long) {
            // no fraction digits; do nothing
            return lnf.format( num.longValue() );
        } else {
            final BigDecimal bigDecimal = (num instanceof BigDecimal)
                    ? (BigDecimal) num
                    : (BigDecimal.valueOf(num.doubleValue()));
            // aka PluralOperand.v
            // this is the # of digits, including trailing zeros, right of the decimal place
            final int v = Math.max(0, bigDecimal.scale());
            // adjust LocalizedNumberFormat
            return lnf.precision( Precision.fixedFraction( v ) )
                    .format( bigDecimal );
        }
    }



}
