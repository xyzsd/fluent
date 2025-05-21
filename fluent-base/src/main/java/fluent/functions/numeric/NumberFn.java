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

package fluent.functions.numeric;

import fluent.functions.*;
import fluent.functions.PluralSelector;
import fluent.syntax.AST.SelectExpression;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/// NUMBER(): Control formatting of numbers.
///
/// This is an Implicit function; it is called any time a number needs to be formatted to a String. When explicit,
/// arguments to control formatting may be supplied.
///
/// As a selector, NUMBER() uses its argument to determine a CLDR plural category for the given locale.
/// By default, this is type:"cardinal". Ordinal types are supported. If it is NOT desirable to produce a plural category
/// from the number, type:"string" can be used; then an exact string pattern match (after formatting) will be performed.
///
/// When used as a selector, decimal formatting can (for some locales) make a difference as to plural selection. If this
/// is important, consider using BigDecimal types, because the decimal representation can be more precisely controlled
/// in contrast to double/float types. For integral types, this is generally less of a concern.
///
/// For a pattern-based alternative to number formatting, see the [DecimalFn].
///
/// Supported format options:
///     - `useGrouping:` "true" or "false". Whether to use a locale-appropriate numeric grouping separator.
///
///     - `minimumIntegerDigits:` Minimum number of digits to display before (left of) the decimal separator.
///
///     - `minimumFractionDigits:` Minimum number of digits to display after the decimal separator.
///
///     - `maximumFractionDigits:` Maximum number of digits to display after the decimal separator.
///
///     - `style:` Numeric display style "decimal", "currency", or "percent". "unit" style is NOT supported.
///
///
///     If either (or both) of the following format options are used, the minimumIntegerDigits, minimumFractionDigits, and
///     maximumFractionDigits are ignored. The following options are:
///
///        - `minimumSignificantDigits:` minimum significant digits (right of decimal separator) to display.
///        - `maximumSignificantDigits:` maximum significant digits (right of decimal separator) to display.
///
///
///      As a selector, the following options are supported (this will be otherwise be ignored):
///
///        - `type`: "cardinal", "ordinal", or "string". No formatting is performed for cardinal or ordinal types,
///     and a CLDR plural category will be selected as appropriate for the number and locale.
///     For string types, formatting is applied then exact-string matching on selector variants occurs.
///
///      Unsupported options:
///        - `unitDisplay`
///        - `currencyDisplay`
///
///
///      General notes:
///        - Extraneous options are ignored.
///        - Rounding mode: RoundingMode.HALF_UP
///
@NullMarked
public enum NumberFn implements FluentFunction, ImplicitFormatter<Number> {

    // loosely based on:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat

    NUMBER;


    @Override
    public String format(final FluentValue<? extends Number> in, final Scope scope) {
        final CustomFormatter formatter = CustomFormatter.of( scope.options(), scope.bundle().locale() );
        return  formatter.format( in.value() );
    }

    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        FluentFunction.ensureInput( parameters );

        final SelectType type = parameters.options()
                .asEnum( SelectType.class, "type" )
                .orElse( SelectType.STRING );

        return applyType( type, parameters, scope );
    }

    @Override
    public FluentValue<?> select(SelectExpression selectExpression, ResolvedParameters parameters, Scope scope) throws FluentFunctionException {
        System.err.println("** number select not yet implemented");
        throw new UnsupportedOperationException("** number select not yet implemented");
    }

    /*
    // will convert FluentNumbers<> to a FluentString with the appropriate plural category
    public List<FluentValue<?>>  select(SelectExpression selectExpression, ResolvedParameters_OLD params, Scope scope) {
        // TODO: reevaluate this
        FluentFunction_OLD.ensureInput( params );

        // for select(), the default action is to format numbers into the cardinal plural type
        final SelectType type = params.options()
                .asEnum( SelectType.class, "type" )
                .orElse( SelectType.CARDINAL );

        return applyType( type, params, scope );
    }
    */

    // apply the type
    private List<FluentValue<?>> applyType(final SelectType type, final ResolvedParameters params, final Scope scope) {
        final PluralSelector pluralSelector = scope.pluralSelector();

        final Function<Number, String> pluralFn = switch(type) {
            case CARDINAL -> pluralSelector::selectCardinal;
            case ORDINAL -> pluralSelector::selectOrdinal;
            case STRING -> {
                final CustomFormatter formatter = CustomFormatter.of( params.options(), scope.bundle().locale() );
                yield formatter::format;
            }
        };

        final var biConsumer = FluentFunction.mapOrPassthrough( Number.class, pluralFn );
        return params.positionals().mapMulti( biConsumer ).toList();
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
            BigDecimal bigDecimal;
            if (number instanceof BigDecimal bdIn) {
                bigDecimal = bdIn.round( mc );
            } else {
                bigDecimal = new BigDecimal( number.toString(), mc );
            }

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


        static CustomFormatter of(final Options options, Locale locale) {
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
                    throw FluentFunctionException.of( "significant digits out of range (<1 or >21) or max < min" );
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

