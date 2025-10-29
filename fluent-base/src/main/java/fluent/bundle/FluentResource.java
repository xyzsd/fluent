/*
 *
 *  Copyright (C) 2021-2025, xyzsd (Zach Del) 
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
import fluent.syntax.parser.FTLParseException;

import java.util.List;

/// Immutable class holding the AST from parsing.
///
/// FluentResources are used to create FluentBundles.
///
/// This is a simple holder of 3 lists:
///     - A List of AST entries
///     - A List of errors encountered during parsing
///     - A list of 'Junk', which represent the text of unparseable sections
///
/// Lists may be empty.
///
@NullMarked
public record FluentResource(List<Entry> entries, List<FTLParseException> errors, List<Junk> junk) {


    public FluentResource {
        entries = List.copyOf(entries);
        errors = List.copyOf(errors);
        junk = List.copyOf(junk);
    }

    // an Empty FluentResource
    public static FluentResource of() {
        return new FluentResource( List.of(), List.of(), List.of() );
    }

    /// True if there are errors
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /// True if there is 'Junk'
    public boolean hasJunk() {
        return !junk.isEmpty();
    }

}
