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
package fluent.function.functions.numeric;

import com.ibm.icu.number.*;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import fluent.bundle.resolver.Scope;
import fluent.function.*;
import fluent.syntax.AST.VariantKey;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;


// 'UNIT' in unitDisplay is nOT supported
// however this does support semantic skeletons

@NullMarked
public enum NumberFn implements FluentFunctionFactory<FluentFunction.Formatter<Number>> {





    NUMBER;

    // constant for percent scaling
    private static final BigDecimal BIG_DECIMAL_100 = new BigDecimal( 100 );

    // IMPLEMENTATION NOTES:
    // Uses ICU NumberFormatter, because it aligns more closely with the JavaScript Intl.NumberFormat
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat
    // and because it is both threadsafe and immutable.
    // It also supports semantic skeletons, which is a powerful formatting method.


    ///  Complicated because Precision has absent chaining for many options, and we are trying to adhere
    ///  to the JS spec for NUMBER
    private static @Nullable Precision precision(final Options options)
            throws IllegalArgumentException, FluentFunctionException {
        // min/max fraction digits must be checked FIRST
        final FractionPrecision fp;
        if (options.has( "minimumFractionDigits" ) && options.has( "maximumFractionDigits" )) {
            // clarify the error message (otherwise, the error message is "Fraction length must be between 0 and 999..."
            // from Precision.minMaxFraction()
            final int min = options.asInt( "minimumFractionDigits" ).orElseThrow();
            final int max = options.asInt( "maximumFractionDigits" ).orElseThrow();

            if (max < min) {
                throw FluentFunctionException.of("minimumFractionDigits must be <= maximumFractionDigits");
            }

            fp = Precision.minMaxFraction(min, max);
        } else if (options.has( "minimumFractionDigits" )) {
            fp = Precision.minFraction( options.asInt( "minimumFractionDigits" ).orElseThrow() );
        } else if (options.has( "maximumFractionDigits" )) {
            fp = Precision.maxFraction( options.asInt( "maximumFractionDigits" ).orElseThrow() );
        } else {
            fp = null;
        }

        // now check sig fig, BUT we need to apply to FractionPrecision if that exists.
        if (options.has( "minimumSignificantDigits" ) || options.has( "maximumSignificantDigits" )) {
            if (options.has( "minimumSignificantDigits" ) && options.has( "maximumSignificantDigits" )) {
                final int minSig = options.asInt( "minimumSignificantDigits" ).orElseThrow();
                final int maxSig = options.asInt( "minimumSignificantDigits" ).orElseThrow();

                if (fp == null) {
                    return Precision.minMaxSignificantDigits( minSig, maxSig );
                } else {
                    return fp.withSignificantDigits( minSig, maxSig, NumberFormatter.RoundingPriority.RELAXED );
                }
            } else if (options.has( "minimumSignificantDigits" )) {
                final int minSig = options.asInt( "minimumSignificantDigits" ).orElseThrow();
                if (fp == null) {
                    return Precision.minSignificantDigits( minSig );
                } else {
                    return fp.withMinDigits( minSig );
                }
            } else {
                final int maxSig = options.asInt( "maximumSignificantDigits" ).orElseThrow();
                if (fp == null) {
                    return Precision.maxSignificantDigits( maxSig );
                } else {
                    return fp.withMaxDigits( maxSig );
                }
            }
        }

        // no sig fig
        return fp;
    }

    @Override
    public FluentFunction.Formatter<Number> create(final Locale locale, final Options options) {
        // 'kind' : used for selectors, but is ignored otherwise.
        final SelectKind kind = options
                .asEnum( SelectKind.class, "kind" )
                .orElse( SelectKind.CARDINAL );

        // 'skeleton' : use format skeletons.
        // if any other number format options are present (other than 'kind'), error
        if (options.has( "skeleton" )) {
            if (options.size() == 1 || (options.size() == 2 && options.has( "kind" ))) {
                // skeleton path
                try {
                    final LocalizedNumberFormatter formatter = NumberFormatter.forSkeleton( options.asString( "skeleton" ).orElse( "" ) )
                            .locale( locale );
                    return NumberFnImpl.of( formatter, locale, kind );
                } catch (SkeletonSyntaxException e) {
                    throw FluentFunctionException.of( e );
                }
            } else {
                // error path: too many options specified.
                throw FluentFunctionException.of( "Too many options; option 'skeleton' can only be used alone, or with 'kind'." );
            }
        }

        // non-skeleton path.
        try {
            LocalizedNumberFormatter formatter = NumberFormatter.withLocale( locale );

            if (options.has( "style" )) {
                // decimal is assumed, no change
                final NFStyle style = options.asEnum( NFStyle.class, "style" ).orElse( NFStyle.DECIMAL );
                if (style == NFStyle.PERCENT) {
                    formatter = formatter.scale( Scale.byBigDecimal( BIG_DECIMAL_100 ) );
                    formatter = formatter.unit( MeasureUnit.PERCENT );
                } else if (style == NFStyle.CURRENCY) {
                    formatter = formatter.unit( Currency.getInstance( locale ) );
                }
            }

            if (options.has( "unitDisplay" )) {
                final NFUnitDisplay unitDisplay = options.asEnum( NFUnitDisplay.class, "unitDisplay" ).orElseThrow();
                formatter = formatter.unitWidth( unitDisplay.unitWidth() );
            }

            if (options.has( "currencyDisplay" )) {
                // overrides 'unitDisplay' if present
                final NFCurrencyDisplay currencyDisplay = options.asEnum( NFCurrencyDisplay.class, "currencyDisplay" ).orElseThrow();
                formatter = formatter.unitWidth( currencyDisplay.unitWidth() );
            }

            if (options.has( "minimumIntegerDigits" )) {
                formatter = formatter.integerWidth( IntegerWidth.zeroFillTo( options.asInt( "minimumIntegerDigits" ).orElseThrow() ) );
            }

            final Precision p = precision( options );
            if (p != null) {
                formatter = formatter.precision( p );
            }

            if (options.has( "useGrouping" )) {
                formatter = formatter.grouping( options.asEnum( GroupingStrategy.class, "useGrouping" )
                        .get() // should not fail
                        .strategy() );
            }

            return NumberFnImpl.of( formatter, locale, kind );
        } catch (IllegalArgumentException e) {
            throw FluentFunctionException.of( e );
        }
    }

    @Override
    public boolean canCache() {
        return true;
    }

    private enum SelectKind {
        // CLDR categories:
        CARDINAL, ORDINAL,
        // ignore CLDR; perform exact match after formatting
        EXACT
    }

    // Intl.NumberFormat -> ICU
    private enum GroupingStrategy {

        // ALWAYS and TRUE are equivalent
        ALWAYS( NumberFormatter.GroupingStrategy.ON_ALIGNED ),
        TRUE( NumberFormatter.GroupingStrategy.ON_ALIGNED ),
        //
        AUTO( NumberFormatter.GroupingStrategy.AUTO ),
        MIN2( NumberFormatter.GroupingStrategy.MIN2 ),
        FALSE( NumberFormatter.GroupingStrategy.OFF );
        // ... should there also be a 'NEVER' ? (== FALSE) for symmetry?

        private final NumberFormatter.GroupingStrategy strategy;

        GroupingStrategy(NumberFormatter.GroupingStrategy strategy) {
            this.strategy = strategy;
        }

        public NumberFormatter.GroupingStrategy strategy() {
            return strategy;
        }
    }


    private enum NFStyle {
        DECIMAL, CURRENCY, PERCENT
    }

    private enum NFUnitDisplay {

        SHORT( NumberFormatter.UnitWidth.SHORT ),
        NARROW( NumberFormatter.UnitWidth.NARROW ),
        LONG( NumberFormatter.UnitWidth.FULL_NAME );

        private final NumberFormatter.UnitWidth unitWidth;

        NFUnitDisplay(NumberFormatter.UnitWidth unitWidth) {
            this.unitWidth = unitWidth;
        }

        public NumberFormatter.UnitWidth unitWidth() {return unitWidth;}
    }

    private enum NFCurrencyDisplay {
        CODE( NumberFormatter.UnitWidth.ISO_CODE ),
        SYMBOL( NumberFormatter.UnitWidth.FORMAL ),
        NARROWSYMBOL( NumberFormatter.UnitWidth.NARROW ),
        NAME( NumberFormatter.UnitWidth.FULL_NAME );

        private final NumberFormatter.UnitWidth unitWidth;

        NFCurrencyDisplay(NumberFormatter.UnitWidth unitWidth) {
            this.unitWidth = unitWidth;
        }

        public NumberFormatter.UnitWidth unitWidth() {return unitWidth;}
    }


    @NullMarked
    private record NumberFnImpl(LocalizedNumberFormatter formatter,
                                @Nullable PluralRules pluralRules) implements FluentFunction.Formatter<Number> {


        static NumberFnImpl of(LocalizedNumberFormatter formatter, Locale locale, final SelectKind selectKind) {
            final PluralRules rules = switch (selectKind) {
                case CARDINAL -> PluralRules.forLocale( locale, PluralRules.PluralType.CARDINAL );
                case ORDINAL -> PluralRules.forLocale( locale, PluralRules.PluralType.ORDINAL );
                case EXACT -> null;
            };
            return new NumberFnImpl( formatter, rules );
        }


        @Override
        public FluentValue<String> format(FluentValue<? extends Number> in, Scope scope) {
            return FluentString.of( formatToString( in.value() ) );
        }

        @Override
        public VariantKey select(final ResolvedParameters parameters, final List<VariantKey> variantKeys,
                                 final VariantKey defaultKey, final Scope scope) {
            final FluentValue<?> fluentValue = Selector.ensureSingle( parameters );
            if (fluentValue instanceof FluentNumber<?> fluentNumber) {
                // first apply formatting, to get a FormattedNumber; then select() based on that.
                final FormattedNumber formattedNumber = formatter.format( fluentNumber.value() );
                final String selectString = (pluralRules == null) ? formattedNumber.toString() : pluralRules.select( formattedNumber );
                return VariantKey.matchKey( selectString, variantKeys, defaultKey );
            } else {
                return defaultKey;
            }
        }

        @Override
        public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
            FluentFunction.ensureInput( parameters );

            final var biConsumer = FluentFunction.mapOrPassthrough( Number.class, this::formatToString );
            return parameters.positionals()
                    .mapMulti( biConsumer )
                    .toList();
        }

        private String formatToString(final Number in) {
            return formatter.format( in ).toString();
        }
    }


}
