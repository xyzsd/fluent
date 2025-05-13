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

package fluent.bundle.resolver;


import fluent.types.FluentError;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * AST Nodes that support resolution into FluentValues should use this interface.
 */
public interface Resolvable {

    /** The maximum number of placeables which can be expanded in a single call to 'formatPattern' */
    int MAX_PLACEABLES = 100;

    /** Unicode bidi isolation characters */
    char FSI = '\u2068';

    /** Unicode bidi isolation characters */
    char PDI = '\u2069';


    // TODO: future..
    //       once sealed classes are complete, it may be better to remove Resolvable
    //       from AST nodes, and just use switch() instead
    /** resolve a node */
    List<FluentValue<?>> resolve(Scope scope);

    /**
     * Convenience method to create an error message (as a FluentError)
     * and insert it into the format stream, in an attempt to preserve
     * the remainder of the message.
     *
     * @param text FluentError message
     * @return Single item list of containing a FluentError
     */
    static List<FluentValue<?>> error(String text) {
        return List.of( FluentError.of( '{' + text + '}' ) );
    }


}
