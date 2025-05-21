package fluent.functions;

import fluent.functions.list.reducer.ListFn;
import fluent.functions.numeric.NumberFn;
import fluent.functions.temporal.TemporalFn;
import org.jspecify.annotations.NullMarked;

import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@NullMarked
public enum FluentFunctions {

    IMPLICITS(Set.of(ListFn.LIST, NumberFn.NUMBER, TemporalFn.TEMPORAL)),
    NUMERIC(Set.of());  // example...
    // Numeric functions

    // Functions that operate over lists

    // String functions

    // Temporal functions




    private final Set<FluentFunction> set;

    FluentFunctions(Set<FluentFunction> set) {
        this.set = set;
    }

    public Set<FluentFunction> functions() { return set; }



}
