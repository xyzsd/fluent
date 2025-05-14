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

import org.jspecify.annotations.NullMarked;

import static java.util.Objects.requireNonNull;

///  Exceptions encountered during processing of references.
@NullMarked
public class ReferenceException extends RuntimeException {


    private ReferenceException(String s) {
        super( s );
    }

    private ReferenceException(String s, Throwable cause) {
        super( s, cause );
    }



    public static ReferenceException unknownMessage(String id) {
        requireNonNull(id);
        return new ReferenceException( "Unknown message: " + id );
    }

    public static ReferenceException unknownTerm(String id) {
        requireNonNull(id);
        return new ReferenceException( "Unknown term: -" + id );
    }

    public static ReferenceException unknownFn(String id) {
        requireNonNull(id);
        return new ReferenceException( "Unknown function: " + id + "()" );
    }

    public static ReferenceException noValue(String id) {
        requireNonNull(id);
        return new ReferenceException( "No value specified for message: " + id );
    }

    public static ReferenceException unknownAttribute(String msg, String attrib) {
        requireNonNull(msg);
        requireNonNull(attrib);
        return new ReferenceException( String.format( "Unknown attribute '%s' for '%s'", attrib, msg ) );
    }

    public static ReferenceException unknownVariable(String id) {
        requireNonNull(id);
        return new ReferenceException( "Unknown variable: $" + id );
    }

    public static ReferenceException duplicateEntry(String id) {
        requireNonNull(id);
        return new ReferenceException( "Duplicate Message or Term: " + id );
    }
}
