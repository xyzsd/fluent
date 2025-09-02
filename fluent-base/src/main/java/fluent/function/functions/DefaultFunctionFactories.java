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
package fluent.function.functions;


import fluent.function.FluentFunctionFactory;
import fluent.function.functions.list.CountFn;
import fluent.function.functions.list.NumSortFn;
import fluent.function.functions.list.StringSortFn;
import fluent.function.functions.list.reducer.ListFn;
import fluent.function.functions.misc.BooleanFn;
import fluent.function.functions.numeric.AbsFn;
import fluent.function.functions.numeric.NumberFn;
import fluent.function.functions.numeric.OffsetFn;
import fluent.function.functions.numeric.SignFn;
import fluent.function.functions.string.CaseFn;
import fluent.function.functions.temporal.*;
import org.jspecify.annotations.NullMarked;

import java.util.HashSet;
import java.util.Set;

@NullMarked
public enum DefaultFunctionFactories {


    ///  The essential default (implicit) functions. Alternatives may be provided, but
    ///  a function factory for List types, FluentNumber, and FluentTemporal must
    ///  be present.
    IMPLICITS( Set.of( ListFn.LIST, NumberFn.NUMBER, DateTimeFn.DATETIME ) ),

    ///  Numeric function factories, *excluding* the default [NumberFn#NUMBER] implicit
    NUMERIC( Set.of( AbsFn.ABS, OffsetFn.OFFSET, SignFn.SIGN ) ),

    ///  String function factories
    STRING( Set.of( CaseFn.CASE ) ),

    /// Function factories that produce functions that operate over lists, *excluding* the default [ListFn#LIST] implicit.
    LIST( Set.of( CountFn.COUNT, NumSortFn.NUMSORT, StringSortFn.STRINGSORT ) ),

    /// Function factories that operate on units of time or durations,
    /// *excluding* the default (implicit) function factory [DateTimeFn#DATETIME]
    TEMPORAL( Set.of( DateFn.DATE, TimeFn.TIME, TemporalFn.TEMPORAL, ExtractTemporalFn.XTEMPORAL ) ),

    /// Miscellaneous function factories
    MISC( Set.of( BooleanFn.BOOLEAN ));


    private final Set<FluentFunctionFactory<?>> functions;

    DefaultFunctionFactories(Set<FluentFunctionFactory<?>> set) {
        this.functions = set;
    }

    /// Set of all factories EXCEPT for implicits (LIST/NUMBER/DATETIME)
    ///
    /// The returned set is modifiable.
    public static Set<FluentFunctionFactory<?>> allNonImplicits() {
        HashSet<FluentFunctionFactory<?>> set = new HashSet<>(12);
        set.addAll( NUMERIC.factories() );
        set.addAll( STRING.factories() );
        set.addAll( LIST.factories() );
        set.addAll( TEMPORAL.factories() );
        set.addAll( MISC.factories() );
        return set;
    }

    /// FluentFunctionFactories for the given category
    public Set<FluentFunctionFactory<?>> factories() {return Set.copyOf( functions );}

}
