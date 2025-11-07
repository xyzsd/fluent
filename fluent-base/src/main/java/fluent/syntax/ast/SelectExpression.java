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

package fluent.syntax.ast;


import org.jspecify.annotations.NullMarked;

import java.util.List;

import static java.util.Objects.requireNonNull;

///  SelectExpression
@NullMarked
public record SelectExpression(Expression selector, List<Variant> variants,
                               Variant defaultVariant) implements Expression {

    public SelectExpression {
        variants = List.copyOf( variants );
    }

    ///  Create a SelectExpression
    public static SelectExpression of(final Expression selector, final List<Variant> variants) {
        for (Variant variant : variants) {
            if (variant.isDefault()) {
                return new SelectExpression( selector, variants, variant );
            }
        }
        throw new IllegalStateException( "Missing default variant" );
    }

    ///  VariantKeys in the order of the listed Variants
    public List<VariantKey> variantKeys() {
        return variants.stream().map( Variant::key ).toList();
    }

    ///  Default VariantKey
    public VariantKey defaultVariantKey() {
        return defaultVariant.key();
    }

    ///  Find a match (case-sensitive exact) or throws an exception if there is no match.
    public Variant variantFromKey(final VariantKey key) {
        requireNonNull( key );
        for (Variant variant : variants) {
            if (variant.key().equals( key )) {
                return variant;
            }
        }
        throw new IllegalArgumentException("No matching variant for key " + key );
    }


    /// Match a variant by name (case-sensitive exact match); if there
    /// is no match, return the default variant.
    ///
    /// @param name name to match (case-sensitive)
    /// @return matching variant, or default variant if there is no match
    public Variant matchOrDefault(final String name) {
        // NOTE: The parser enforces that there is always one and only one default Variant.
        for (Variant variant : variants) {
            if (variant.key().name().equals( name )) {
                return variant;
            }
        }

        return defaultVariant;
    }




}
