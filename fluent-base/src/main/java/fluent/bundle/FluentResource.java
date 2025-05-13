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
package fluent.bundle;

import org.jspecify.annotations.NullMarked;

import fluent.syntax.AST.Entry;
import fluent.syntax.AST.Junk;
import fluent.syntax.parser.ParseException;

import java.util.List;

/**
 * Immutable class holding the AST from parsing.
 * <p>
 * AST Entries are stored in a List, as are errors and (optionally) Junk.
 * </p>
 * <p>
 *     FluentResources are used to create FluentBundles.
 * </p>
 */
@NullMarked
public record FluentResource(List<Entry> entries, List<ParseException> errors, List<Junk> junk) {


    public FluentResource(List<Entry> entries, List<ParseException> errors, List<Junk> junk) {
        this.entries = List.copyOf(entries);
        this.errors = List.copyOf(errors);
        this.junk = List.copyOf(junk);
    }

    /**
     * True if there are errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * True if there is 'Junk'
     */
    public boolean hasJunk() {
        return !junk.isEmpty();
    }

}
