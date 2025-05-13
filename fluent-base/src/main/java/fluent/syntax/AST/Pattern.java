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

package fluent.syntax.AST;

import fluent.bundle.resolver.Resolvable;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentString;
import fluent.types.FluentValue;

import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public record Pattern(List<PatternElement> elements) implements SyntaxNode, Resolvable {

    // initial size of StringBuilder (todo: determine a good initial size or perhaps allow tuning)
    private static final int SB_SIZE = 128;

    public Pattern {
        elements = List.copyOf( elements );
    }


    @Override
    public List<FluentValue<?>> resolve(Scope scope) {
        if (scope.isDirty()) {
            return Resolvable.error( "[dirty]" );
        }

        // fast-path (resolve)
        if (elements.size() == 1) {
            final PatternElement patternElement = elements.get( 0 );
            if (patternElement instanceof PatternElement.TextElement textElement) {
                return List.of( FluentString.of( textElement.value() ) );
            } else {
                return scope.maybeTrack( this, ((PatternElement.Placeable) patternElement) );
            }
        }

        StringBuilder sb = new StringBuilder( SB_SIZE );
        for (PatternElement patternElement : elements) {
            if (scope.isDirty()) {
                return Resolvable.error( "[dirty]" );
            }

            if (patternElement instanceof PatternElement.TextElement textElement) {
                sb.append( textElement.value() );
            } else if (patternElement instanceof PatternElement.Placeable placeable) {
                scope.incrementAndCheckPlaceables();

                final boolean needsIsolation = scope.bundle().useIsolation()
                        && elements.size() > 1
                        && placeable.needsIsolation();

                if (needsIsolation) {
                    sb.append( Resolvable.FSI );
                }

                final List<FluentValue<?>> fluentValues = scope.maybeTrack( this, placeable );
                sb.append( scope.reduce( fluentValues ) );

                if (needsIsolation) {
                    sb.append( Resolvable.PDI );
                }
            } else {
                throw new IllegalStateException( patternElement.toString() );
            }
        }

        return List.of( FluentString.of( sb.toString() ) );
    }

}
