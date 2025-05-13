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


import fluent.functions.FluentImplicit;
import fluent.functions.ResolvedParameters;
import fluent.bundle.resolver.Scope;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

@NullMarked
public final record SelectExpression(Expression selector,
                                     List<Variant> variants) implements Expression {

    public SelectExpression {
        variants = List.copyOf( variants );
    }

    /**
     * Returns the default variant for this SelectExpression
     */
    public Variant defaultVariant() {
        return variants.stream()
                .filter( Variant::isDefault )
                .findFirst()
                // the following should not occur, as the parser ensures there is a default
                .orElseThrow( () -> new IllegalStateException( "Missing default!" ) );
    }


    /**
     * Match a variant based on name.
     */
    public Optional<Variant> matchVariant(final String s) {
        return variants.stream()
                .filter( variant -> variant.key().equals( s ) )
                .findFirst();
    }


    /**
     * Match a variant by name (case-sensitive exact match); if there
     * is no match, return the default variant.
     *
     * @param name name to match (case sensitive)
     * @return matching variant, or default variant if there is no match
     */
    public Variant matchOrDefault(final String name) {
        Variant defaultVariant = null;
        for (Variant v : variants) {
            if (v.key().equals( name )) {
                return v;
            }

            if (v.isDefault() && defaultVariant == null) {
                defaultVariant = v;
            }
        }

        // this should not occur ... presence of a default is enforced by the parser
        if (defaultVariant == null) {
            throw new IllegalStateException( "Missing default!" );
        }

        return defaultVariant;
    }


    @Override
    public List<FluentValue<?>> resolve(final Scope scope) {
        final List<FluentValue<?>> resolved = selector.resolve( scope );

        return scope.bundle().implicit( FluentImplicit.Implicit.JOIN )
                .select(
                        this,
                        ResolvedParameters.from( resolved, scope ),
                        scope
                );
    }


}
