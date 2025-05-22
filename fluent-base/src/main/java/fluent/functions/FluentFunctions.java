package fluent.functions;

import fluent.functions.list.CountFn;
import fluent.functions.list.NumSortFn;
import fluent.functions.list.StringSortFn;
import fluent.functions.list.reducer.ListFn;
import fluent.functions.numeric.*;
import fluent.functions.string.CaseFn;
import fluent.functions.temporal.TemporalFn;
import org.jspecify.annotations.NullMarked;

import java.util.HashSet;
import java.util.Set;

@NullMarked
public enum FluentFunctions {
    // todo: determine if we really want implicits in the other categories

    ///  The esential implicits. Alternatives may be provided, but
    ///  Functions/Implicits for List types, FluentNumber, and FluentTemporal must
    ///  be present.
    IMPLICITS( Set.of( ListFn.LIST, NumberFn.NUMBER, TemporalFn.TEMPORAL ) ),

    ///  Numeric functions
    NUMERIC( Set.of( AbsFn.ABS, AddFn.IADD, CompactFn.COMPACT, CurrencyFn.CURRENCY, DecimalFn.DECIMAL, NumberFn.NUMBER, SignFn.SIGN ) ),

    ///  String functions
    STRING( Set.of( CaseFn.CASE ) ),

    /// Functions that operate over lists
    LIST( Set.of( ListFn.LIST, CountFn.COUNT, NumSortFn.NUMSORT, StringSortFn.STRINGSORT ) ),

    /// Functions that may operate on units of time or durations
    TEMPORAL( Set.of( TemporalFn.TEMPORAL ) );


    ///  Complete set without the required implicits specified in IMPLICITS
    public static final Set<FluentFunction> ALL_NONIMPLICIT = makeSet();

    private final Set<FluentFunction> functions;

    FluentFunctions(Set<FluentFunction> set) {
        this.functions = set;
    }

    private static Set<FluentFunction> makeSet() {
        HashSet<FluentFunction> set = new HashSet<>();
        for (FluentFunctions ffns : values()) {
            set.addAll( ffns.functions );
        }
        set.removeAll( IMPLICITS.functions );
        return Set.copyOf( set );
    }

    public Set<FluentFunction> functions() {return functions;}

}
