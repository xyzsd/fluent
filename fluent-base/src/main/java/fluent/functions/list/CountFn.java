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

package fluent.functions.list;

import fluent.functions.*;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentNumber;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 *  COUNT() : Counts the supplied arguments.
 *  <p>
 *      If supplied arguments are Lists, items in the lists are also counted.
 *  </p>
 *  <p>
 *      Examples:
 *      <ul>
 *      <li>COUNT() => 0</li>
 *      <li>COUNT("hello") => 1</li>
 *      <li>COUNT(7) => 1</li>
 *      <li>COUNT("hello", 7) => 2</li>
 *      <li>COUNT($myvar) where $myvar=List.of("item","another","yet another") => 3</li>
 *       <li>COUNT($myvar, "hello") where $myvar=List.of("item","another","yet another") => 4</li>
 *       </ul>
 *  This is a reducing function. Count returns a single number as its output.
 *  If any arguments are errors, this will fail.
 *
 */
@NullMarked
public enum CountFn implements FluentFunction {


    COUNT;



    @Override
    public List<FluentValue<?>> apply(final ResolvedParameters parameters, final Scope scope) throws FluentFunctionException {
        if (!parameters.hasPositionals()) {
            return List.of( FluentNumber.of( 0L ) );
        }

        // invalid values result in an exception
        parameters.positionals().forEach( FluentFunction::validate );

        return List.of( FluentNumber.of(
                parameters.positionals().count()
        ) );
    }
}
