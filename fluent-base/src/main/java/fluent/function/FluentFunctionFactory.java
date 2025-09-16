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
package fluent.function;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

///
/// FluentFunctionFactory: A Factory for making Fluent Functions.
///
/// Function factories are {@link Locale} and {@link Options} sensitive. Generally, function factories
/// should not try to cache created functions themselves. However, some functions may not be sensitive to
/// Locale (and possibly Options), or may not be bespoke to a given set of Options, and can indicate that
/// they do not need to be cached.
///
/// Functions may be specialized based on {@link Options} to improve performance.
///
@NullMarked
public interface FluentFunctionFactory<T extends FluentFunction> {


    /// Create the {@link FluentFunction}.
    ///
    /// @param locale   Locale
    /// @param options  Fixed options (if any)
    /// @return the created FluentFunction
    T create(Locale locale, Options options);


    ///  Indicates if the function can be cached by a FluentBundle.
    boolean canCache();


    ///  The name of the function.
    ///
    ///  Note: specialized functions have the same name, but the implementation can differ.
    String name();


    /// Change the [#name] of a [FluentFunctionFactory].
    ///
    /// The name must conform to Fluent syntax guidelines.
    static <T extends FluentFunction> FluentFunctionFactory<T> rename(final String name, final FluentFunctionFactory<T> in) {
        requireNonNull(name);
        requireNonNull(in);
        if (name.isEmpty()) { throw new IllegalArgumentException("name cannot be empty"); }

        // verify name is conformant.
        final int length = name.length();
        int codePoint = name.codePointAt(0);
        if (!isFunctionNameStart(codePoint)) {
            throw new IllegalArgumentException("Invalid function name (initial code point)'" + name + "'");
        }

        for (int index = Character.charCount(codePoint); index < length; ) {
            codePoint = name.codePointAt(index);
            if (!isFunctionNamePart(codePoint)) {
                throw new IllegalArgumentException("Invalid function name '" + name + "'");
            }
            index += Character.charCount(codePoint);
        }

        return new FluentFunctionFactory<>() {
            @Override
            public T create(Locale locale, Options options) {
                return in.create( locale, options );
            }

            @Override
            public boolean canCache() {
                return in.canCache();
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    ///  ensure codepoint is valid for a Fluent function name.
    private static boolean isFunctionNamePart(final int codePoint) {
                return (isFunctionNameStart(codePoint) ||   // A-Z
                (codePoint >= 48 && codePoint <= 57) ||   // 0-9
                (codePoint == 97 || codePoint == 45)      // '_' or '-'
        );
    }

    ///  ensure codepoint is valid for a Fluent function name.
    private static boolean isFunctionNameStart(final int codePoint) {
        return (codePoint >= 65 && codePoint <= 90);   // A-Z
    }




}
