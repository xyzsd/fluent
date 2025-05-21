package fluent.bundle.resolver;

import fluent.functions.*;
import fluent.syntax.AST.SelectExpression;
import fluent.syntax.AST.Variant;
import fluent.types.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

///  could have separate formatter for each type e.g.
///
///  FluentValue Formatter/Selector
///  formatter for implicit values (no function specified)
@NullMarked
public class FluentValueFormatter {

    // we have to make sure these are typesafe
    private final List<C2FEntry<?>> customList;
    private final Map<Class<?>, C2FEntry<?>> customExact;
    private final boolean hasCustoms;   // if either above NOT EMPTY this is true

    // required implicit formatters
    private final TerminalReducer reducer;
    private final ImplicitFormatter<TemporalAccessor> temporalFormatter;
    private final Options temporalOptions;
    private final ImplicitFormatter<Number> numberFormatter;
    private final Options numberOptions;

    private FluentValueFormatter(Builder b) {
        customList = List.copyOf( b.subtypes );
        customExact = Map.copyOf( b.exact );

        reducer = requireNonNull( b.reducer );
        temporalFormatter = requireNonNull( b.temporalFormatter );
        temporalOptions = b.temporalOptions;
        numberFormatter = requireNonNull( b.numberFormatter );
        numberOptions = b.numberOptions;

        hasCustoms = !(customExact.isEmpty() && customList.isEmpty());
    }

    public static Builder builder() {
        return new Builder();
    }


    /// Handle fluent 'select'
    ///
    /// By default, this method picks the default variant. No attempt to match variants is performed.
    ///
    /// @param selectExpression SelectExpression
    /// @param params           ResolvedParameters
    /// @param scope            Scope
    /// @return Variant
    // TODO: probably should move select() out of this class
    public static Variant select(final FluentValue<?> fv, final SelectExpression selectExpression, final ResolvedParameters params, final Scope scope) {
        return switch (fv) {
            // Select variant that exactly matches this FluentString.
            case FluentString(String value) -> selectExpression.matchOrDefault( value );
            // number
            // By default, for select statements, attempt to match a Variant that corresponds to the
            // cardinal plural form of this number. If there is no match, the default Variant is returned.
            case FluentNumber<? extends Number> fluentNumber -> {
                final String categoryName = scope.pluralSelector().selectCardinal( fluentNumber.value() );
                yield selectExpression.matchOrDefault( categoryName );
            }
            // no attempt to match
            default -> selectExpression.defaultVariant();
        };
    }


    /// Formats a FluentValue to a `FluentValue<String>`
    ///
    /// @param scope Scope
    /// @return FluentString or FluentError. Never null.
    public String format(FluentValue<?> fv, Scope scope) {
        // TODO: does not yet handle options in formatters
        // TODO:  may need 'options' as argument to ImplicitFormatter.format() (called 'Options implicitOptions')
        return switch (fv) {
            case FluentString s -> s.value();
            case FluentError e -> e.value();
            case FluentNumber<? extends Number> fluentNumber -> numberFormatter.format( fluentNumber, scope );
            case FluentTemporal fluentTemporal -> temporalFormatter.format( fluentTemporal, scope );
            case FluentCustom<?> custom when hasCustoms -> formatCustom( custom, scope );
            // failsafe custom formatter
            case FluentCustom<?> custom -> defaultFluentCustomFormatter( custom );
        };
    }


    ///  The reducer
    public TerminalReducer reducer() {
        return reducer;
    }

    public Options temporalOptions() {
        return temporalOptions;
    }

    public Options numberOptions() {
        return numberOptions;
    }


    ///  just convert to a String.
    private String defaultFluentCustomFormatter(final FluentCustom<?> any) {
        return String.valueOf( any.value() );
    }

    private <T> String formatCustom(FluentCustom<T> custom, Scope scope) {
        // TODO: need to add options when formatting; probably should pass to Implicit formatter
        //
        final C2FEntry<T> entry = this.lookup( custom.type() );
        if (entry != null) {
            return entry.ffmt().format( custom, scope );
        }

        // no custom formatter found. use default
        return defaultFluentCustomFormatter( custom );
    }


    @SuppressWarnings("unchecked")
    @Nullable
    private <T> C2FEntry<T> lookup(Class<T> cls) {
        // exact
        final C2FEntry<?> found = customExact.get( cls );
        if (found != null) {
            return (C2FEntry<T>) found;
        }

        // inexact
        for (final C2FEntry<?> entry : customList) {
            if (cls.isAssignableFrom( entry.cls() )) {
                return (C2FEntry<T>) entry;
            }
        }

        // no match
        return null;
    }

    // for entries of formatters in the list
    private record C2FEntry<T>(Class<T> cls, ImplicitFormatter<T> ffmt,
                               Options opts) {}


    ///  Build a FluentValueFormatter
    public static class Builder {
        // we have to make sure these are typesafe
        private final Map<Class<?>, C2FEntry<?>> exact;
        private final List<C2FEntry<?>> subtypes;

        //private final List<C2FEntry<FluentCustom<?>>> customList;
        //private final Map<Class<?>, C2FEntry<FluentCustom<?>>> customExact;

        // specific formatters
        @Nullable private TerminalReducer reducer;
        @Nullable private ImplicitFormatter<TemporalAccessor> temporalFormatter;
        @Nullable private ImplicitFormatter<Number> numberFormatter;
        private Options temporalOptions = Options.EMPTY;
        private Options numberOptions = Options.EMPTY;

        public Builder() {
            exact = new HashMap<>();
            subtypes = new ArrayList<>();
        }

        public Builder setTerminalReducer(TerminalReducer reducer) {
            this.reducer = requireNonNull( reducer );
            return this;
        }

        public Builder setTemporalFormatter(ImplicitFormatter<TemporalAccessor> temporalFormatter, Options opts) {
            this.temporalFormatter = requireNonNull( temporalFormatter );
            this.temporalOptions = requireNonNull( opts );
            return this;
        }

        public Builder setNumberFormatter(ImplicitFormatter<Number> numberFormatter, Options opts) {
            this.numberFormatter = requireNonNull( numberFormatter );
            this.numberOptions = requireNonNull( opts );
            return this;
        }


        public <T> Builder setCustomFormatterExact(Class<T> cls, ImplicitFormatter<T> ffmt, Options opts) {
            requireNonNull( cls );
            requireNonNull( ffmt );
            requireNonNull( ffmt );
            exact.put( cls, new C2FEntry<>( cls, ffmt, opts ) );
            return this;
        }

        // this looks up the function (error if not present) and then calls the FluentFormatter (with options if specified)
        public Builder setCustomFormatterExact(Class<?> cls, String fnName, Options opts) {
            requireNonNull( cls );
            requireNonNull( fnName );
            requireNonNull( opts );
            throw new UnsupportedOperationException( "NOT IMPLEMENTED YET" );
            // TODO: implement
            // need to lookup function; fail if not available (throw exception)
            // TODO: shoudl DEFER until build(), that way we can wait until we have all the
            //  necessary info in fluentbundle
            // options -- when built -- are combined with global options, to save one 'merge' operation.
        }

        // simple lambda; converted into a FluentFunction.Formatter automatically
        // this is an easy way to create simple implicit formatters
        public <T> Builder setCustomFormatterExact(Class<T> cls, BiFunction<T, Scope, String> fn, Options opts) {
            ImplicitFormatter<T> ffmt = (in, scope) ->
                    fn.apply( in.value(), scope );
            setCustomFormatterExact( cls, ffmt, opts );
            return this;
        }


        // Unlike exact(), these also apply to subclasses (via a list and comparison using isAssignable...)
        // tried after exact match, and are order-dependent (tested against the class in the
        // order they were added)
        public <T> Builder setCustomFormatter(Class<T> cls, ImplicitFormatter<T> ffmt, Options opts) {
            requireNonNull( cls );
            requireNonNull( ffmt );
            requireNonNull( ffmt );
            subtypes.add( new C2FEntry<>( cls, ffmt, opts ) );
            return this;
        }

        // this looks up the function (error if not present) and then calls the FluentFormatter (with options if specified)
        public Builder setCustomFormatter(Class<?> cls, String fnName, Options opts) {
            requireNonNull( cls );
            requireNonNull( fnName );
            requireNonNull( opts );
            throw new UnsupportedOperationException( "NOT IMPLEMENTED YET" );
            // TODO: implement
        }

        // simple lambda; converted into a FluentFunction.Formatter automatically
        // this is an easy way to create simple implicit formatters
        public <T> Builder setCustomFormatter(Class<T> cls, BiFunction<T, Scope, String> fn, Options opts) {
            ImplicitFormatter<T> ffmt = (in, scope) ->
                     fn.apply( in.value(), scope );
            setCustomFormatter( cls, ffmt, opts );
            return this;
        }


        // to build, we need implicit names from bundle builder (to lookup any functions),
        public FluentValueFormatter build() {
            Objects.requireNonNull( reducer, "reducer (list formatter) not set" );
            Objects.requireNonNull( temporalFormatter, "default FluentTemporal formatter not set" );
            Objects.requireNonNull( numberFormatter, "default FluentNumber formatter not set" );


            return new FluentValueFormatter( this );
        }

    }
}
