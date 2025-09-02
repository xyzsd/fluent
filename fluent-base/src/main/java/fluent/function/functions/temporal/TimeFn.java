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
package fluent.function.functions.temporal;

import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import org.jspecify.annotations.NullMarked;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

// time-only format; one option : 'style', and zone override 'zone'
@NullMarked
public enum TimeFn implements FluentFunctionFactory<FluentFunction.Formatter<TemporalAccessor>> {

    TIME;


    @Override
    public FluentFunction.Formatter<TemporalAccessor> create(final Locale locale, final Options options) {
        // optional
        final FormatStyle style = options.asEnum( FormatStyle.class, "style" )
                .orElse( FormatStyle.MEDIUM );

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime( style );

        // optional ('zone:')
        formatter = TemporalFn.parseZone( formatter, options );

        return new TemporalFn.DTF( formatter );
    }

    @Override
    public boolean canCache() {
        return true;
    }

}
