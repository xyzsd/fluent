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
package fluent.function.functions.misc;

import fluent.function.*;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;
import java.util.function.Function;

/// ## BOOLEAN()
/// A function that operates on `FluentCustom<Boolean>` values.
///
/// Note: this applies to Boolean values, not Strings!
///
/// This is most useful for `select` clauses; the select variants would be keyed as `true` or `false`.
///
/// Non-booleans are passed through.
///
/// ## Options
/// -**as**: `string` (default) or `number`. If `number` is use,d the value is converted to a 0 or 1 as a FluentLong.
/// This value could be further manipulated, for example, such as adjusting it with the
/// [OFFSET][fluent.function.functions.numeric.OffsetFn] function.
///
/// ## Examples
/// {@snippet :
///     BOOLEAN($val)                   // if val is Boolean.TRUE, result is "true" (as a string)
///     BOOLEAN($val, as:"number")      // if val is Boolean.TRUE, result is "1" (as a number)
///
///     OFFSET( BOOLEAN($val, as:"number"), increment:7)     // if $val is true, result is '8' otherwise '7'
///
///     BOOLEAN("true")                 // ERROR: 'true' is a String, not a boolean.
///     BOOLEAN(1)                      // ERROR: 1 is a number, not a boolean
/// }
///
/// ## Notes
/// If `as:"number"` is set as a default argument in the function registry, or if
/// `BOOLEAN($arg, as:"number")` is used as a selector (rather than the default `as:"string"`),
/// the returned FluentLong value (0 or 1) will undergo [NUMBER][fluent.function.functions.numeric.NumberFn] selection
/// according to plural rules.
///
@NullMarked
public enum BooleanFn implements FluentFunctionFactory<FluentFunction.Transform> {

    BOOLEAN;

    private static final Function<Boolean, FluentValue<?>> BOOL2STRING = ( b) -> FluentString.of( String.valueOf( b ) );
    private static final Function<Boolean, FluentValue<?>> BOOL2LONG = ( b) ->  new FluentNumber.FluentLong( b ? 1L : 0L );

    @Override
    public FluentFunction.Transform create(Locale locale, Options options) {
        final DisplayAs displayAs = options
                .asEnum( DisplayAs.class, "as" )
                .orElse( DisplayAs.STRING );

        return FluentFunction.passthroughTransform( Boolean.class, transformFor(displayAs) );
    }

    @Override
    public boolean canCache() {
        return true;
    }

    /// create the transform, which is dependent upon the 'as' option.
    private static Function<Boolean,?> transformFor(final DisplayAs displayAs) {
        return switch( displayAs) {
            case STRING -> BOOL2STRING;
            case NUMBER -> BOOL2LONG;
        };
    }


    // for option parsing
    private enum DisplayAs {
        NUMBER, STRING
    }


}
