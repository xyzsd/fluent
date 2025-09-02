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

package fluent.syntax.AST;

import org.jspecify.annotations.NullMarked;

///  Comment nodes
@NullMarked
public sealed interface Commentary extends Entry {

    /// Comment text
    String text();

    ///   Single-hash comments '#'; these can be bound to
    ///   Messages or Terms, but also can be standalone
    record Comment(String text) implements Commentary {}

    ///  Double-Hash '##' comments (Group Comments), which are standalone
    record GroupComment(String text) implements Commentary {
        public GroupComment(Commentary c) {
            this( c.text() );
        }
    }

    ///  Triple-Hash '###' comments (Resource Comments), which are standalone
    record ResourceComment(String text) implements Commentary {
        public ResourceComment(Commentary c) {
            this( c.text() );
        }
    }
}
