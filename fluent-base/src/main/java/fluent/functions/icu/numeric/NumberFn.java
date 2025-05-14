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

package fluent.functions.icu.numeric;

import fluent.functions.*;
import fluent.functions.icu.ICUPluralSelector;
import fluent.syntax.AST.SelectExpression;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * NUMBER(): Control formatting of numbers.
 * <p>
 * This is an Implicit function; it is called any time a number needs to be formatted to a String. When explicit,
 * arguments to control formatting may be supplied.
 * </p>
 * <p>
 * As a selector, NUMBER() uses its argument to determine a CLDR plural category for the given locale.
 * By default, this is type:"cardinal". Ordinal types are supported. If it is NOT desirable to produce a plural category
 * from the number, type:"string" can be used; then an exact string pattern match (after formatting) will be performed.
 * </p>
 * <p>
 * When used as a selector, decimal formatting can (for some locales) make a difference as to plural selection. If this
 * is important, consider using BigDecimal types, because the decimal representation can be more precisely controlled
 * in contrast to double/float types. For integral types, this is generally less of a concern.
 * </p>
 * <p>
 * For a pattern-based alternative to number formatting, see the {@link DecimalFn}.
 * </p>
 *
 * <p>
 * Supported format options:
 *      <ul>
 *          <li>{@code useGrouping:} "true" or "false". Whether to use a locale-appropriate numeric grouping separator.</li>
 *          <li>{@code minimumIntegerDigits:} Minimum number of digits to display before (left of) the decimal separator.</li>
 *          <li>{@code minimumFractionDigits:} Minimum number of digits to display after the decimal separator.</li>
 *          <li>{@code maximumFractionDigits:} Maximum number of digits to display after the decimal separator.</li>
 *          <li>{@code style:} Numeric display style "decimal", "currency", or "percent". "unit" style is NOT supported.</li>
 *      </ul>
 * <p>
 *     If either (or both) of the following format options are used, the minimumIntegerDigits, minimumFractionDigits, and
 *     maximumFractionDigits are ignored. The following options are:
 *     <ul>
 *          <li>{@code minimumSignificantDigits:} minimum significant digits (right of decimal separator) to display.</li>
 *          <li>{@code maximumSignificantDigits:} maximum significant digits (right of decimal separator) to display.</li>
 *      </ul>
 * <p>
 *      As a selector, the following options are supported (this will be otherwise be ignored):
 *      <ul>
 *          <li>
 *              {@code type}: "cardinal", "ordinal", or "string". No formatting is performed for cardinal or ordinal types,
 *              and a CLDR plural category will be selected as appropriate for the number and locale.
 *              For string types, formatting is applied then exact-string matching on selector variants occurs.
 *          </li>
 *      </ul>
 *  <p>
 *      Unsupported options:
 *      <ul>
 *          <li>{@code unitDisplay}</li>
 *          <li>{@code currencyDisplay}</li>
 *      </ul>
 *  <p>
 *      General notes:
 *      <ul>
 *          <li>Extraneous options are ignored.</li>
 *          <li>Rounding mode: RoundingMode.HALF_UP</li>
 *      </ul>
 */
public class NumberFn implements FluentImplicit, ImplicitFormatter {

    // loosely based on:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat

    public NumberFn() {}

    @Override
    public Implicit id() {
        return Implicit.NUMBER;
    }


    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters params, final Scope scope) {
        FluentFunction.ensureInput( params );

        // for apply(), the default action is to format numbers into Strings
        final SelectType type = params.options()
                .asEnum( SelectType.class, "type" )
                .orElse( SelectType.STRING );

        return applyType( type, params, scope );
    }


    // will convert FluentNumbers<> to a FluentString with the appropriate plural category
    @Override
    public List<FluentValue<?>>  select(SelectExpression selectExpression, ResolvedParameters params, Scope scope) {
        FluentFunction.ensureInput( params );

        // for select(), the default action is to format numbers into the cardinal plural type
        final SelectType type = params.options()
                .asEnum( SelectType.class, "type" )
                .orElse( SelectType.CARDINAL );

        return applyType( type, params, scope );
    }

    // apply the type
    private List<FluentValue<?>> applyType(final SelectType type, final ResolvedParameters params, final Scope scope) {
        final ICUPluralSelector pluralSelector = (ICUPluralSelector) scope.fnResources;

        final Function<Number, String> pluralFn = switch(type) {
            case CARDINAL -> pluralSelector::selectCardinal;
            case ORDINAL -> pluralSelector::selectOrdinal;
            case STRING -> {
                final CustomFormatter formatter = CustomFormatter.create( params.options(), scope.bundle().locale() );
                yield formatter::format;
            }
        };

        return FluentFunction.mapOverNumbers( params.valuesAll(), scope, pluralFn );
    }


    // this will fail if 'in' is not a FluentNumber<>
    @Override
    public String format(FluentValue<?> in, Scope scope) {
        assert (in instanceof FluentNumber<?>);
        final CustomFormatter formatter = CustomFormatter.create( scope.options(), scope.bundle().locale() );
        return formatter.format( ((FluentNumber<?>) in).value() );
    }


    private enum SelectType {
        // CLDR categories:
        CARDINAL, ORDINAL,
        // ignore CLDR; perform exact match after formatting
        STRING
    }

    private enum NFStyle {
        DECIMAL, CURRENCY, PERCENT
    }

    // custom formatter, with backup for sigfig, not threadsafe
    private static class CustomFormatter {

        private final NumberFormat formatter;
        private final int minSig;
        private final int maxSig;

        private CustomFormatter(NumberFormat primary, int minSig, int maxSig) {
            this.formatter = primary;
            this.minSig = minSig;
            this.maxSig = maxSig;
        }

        private CustomFormatter(NumberFormat primary) {
            this.formatter = primary;
            this.minSig = -1;
            this.maxSig = -1;
        }


        String format(final Number number) {
            if (useSigFig()) {
                return sigFigFormat(number);
            } else {
                return formatter.format( number );
            }
        }

        private String sigFigFormat(final Number number) {
            // if not finite, pass-through (BigDecimal doesn't support NaN/Infinity)
            if (!isFinite( number )) {
                return formatter.format( number );
            }

            // MathContext precision: sets maximum precision; (default rounding is half-up)
            // this nicely lowers precision from the given input if we need to
            // NOTE: when we create a BigDecimal, precision() can be different than what we specify here
            final MathContext mc = new MathContext( maxSig );
            BigDecimal bigDecimal = null;
            if (number instanceof BigDecimal bdIn) {
                bigDecimal = bdIn.round( mc );
            } else {
                bigDecimal = new BigDecimal( number.toString(), mc );
            }
            assert (bigDecimal != null);

            // add extra zeroes if precision() of the created BigDecimal < minSig
            // e.g.: minSig=12, maxSig=21
            // input: 123456.789        << precision 9, scale 3
            // output: (after below) : 123,456.789000  << adjusted
            if (bigDecimal.precision() < minSig && bigDecimal.scale() >= 0) {
                final int newScale = bigDecimal.precision() - bigDecimal.scale();
                bigDecimal = bigDecimal.setScale( newScale, RoundingMode.HALF_UP );

                // formatter won't respect trailing digits without this setting minimum fraction digits too
                final int minFDOriginal = formatter.getMinimumFractionDigits();
                formatter.setMinimumFractionDigits(newScale);
                final String result = formatter.format( bigDecimal );
                formatter.setMinimumFractionDigits(minFDOriginal);
                return result;
            } else {
                return formatter.format(bigDecimal );
            }
        }

        private boolean useSigFig() {
            return (minSig != -1 && maxSig != -1);
        }

        private boolean isFinite(Number number) {
            assert !(number instanceof Float);
            return (number instanceof Double dbl && Double.isFinite( dbl ));
        }


        static CustomFormatter create(final Options options, Locale locale) {
            // fast path
            if (options.isEmpty()) {
                final DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance( locale );
                df.setRoundingMode( RoundingMode.HALF_UP );
                return new CustomFormatter( df );
            }

            // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat#parameters
            final NFStyle style = options.asEnum( NFStyle.class, "style" ).orElse( NFStyle.DECIMAL );
            final NumberFormat nf = switch (style) {
                case DECIMAL -> NumberFormat.getNumberInstance( locale );
                case CURRENCY -> NumberFormat.getCurrencyInstance( locale );
                case PERCENT -> NumberFormat.getPercentInstance( locale );
            };

            nf.setRoundingMode( RoundingMode.HALF_UP );

            options.asBoolean( "useGrouping" ).ifPresent( nf::setGroupingUsed );

            // alternate path for significant figures; the other options below this block are then ignored (per spec)
            // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat
            if (options.has( "minimumSignificantDigits" ) || options.has( "maximumSignificantDigits" )) {
                final int minSig = options.asInt( "minimumSignificantDigits" ).orElse( 1 );
                final int maxSig = options.asInt( "maximumSignificantDigits" ).orElse( 21 );
                if (minSig < 1 || maxSig > 21 || minSig > maxSig) {
                    throw FluentFunctionException.create( "significant digits out of range (<1 or >21) or max < min" );
                }

                return new CustomFormatter( nf, minSig, maxSig );
            } else {
                // no-sig-fig path

                nf.setMinimumIntegerDigits( options.asInt( "minimumIntegerDigits" ).stream()
                        .filter( v -> v >= 1 && v <= 21 )
                        .findAny()
                        .orElse( nf.getMinimumIntegerDigits() )
                );

                nf.setMinimumFractionDigits( options.asInt( "minimumFractionDigits" ).stream()
                        .filter( v -> v >= 0 && v <= 21 )
                        .findAny()
                        .orElse( nf.getMinimumFractionDigits() )
                );

                nf.setMaximumFractionDigits( options.asInt( "maximumFractionDigits" ).stream()
                        .filter( v -> v >= 0 && v <= 21 && v >= nf.getMinimumFractionDigits() )
                        .findAny()
                        .orElse( Math.max( nf.getMinimumFractionDigits(), nf.getMaximumFractionDigits() ) )
                );

                return new CustomFormatter( nf );
            }
        }
    }



}

