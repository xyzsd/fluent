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

package fluent.functions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * FluentFunctionExceptions are thrown during function evaluation when an error is encountered.
 * <p>
 * Any other expected exception--checked or unchecked-- reasonbly expected during function evaluation
 * should be wrapped in a FluentFunctionException.
 *
 */
public class FluentFunctionException extends RuntimeException {

    @Nullable private final String name;

    private FluentFunctionException(@Nullable String fnName, String message, Throwable cause) {
        super( message, cause );
        this.name = fnName;
    }

    private FluentFunctionException(String message) {
        super( message );
        this.name = null;
    }


    /**
     * The name of the function causing the exception, if set.
     */
    public Optional<String> fnName() {
        return Optional.ofNullable( name );
    }


    @Override
    public String getMessage() {
        return (name == null) ? super.getMessage() : (name + "(): " + super.getMessage());
    }

    /**
     * Add the function name to the exception, creating a new Exception (but maintaining the stack trace).
     * Subsequence calls to getMessage() will prepend the function name to the existing message.
     */
    public FluentFunctionException withName(String functionName) {
        return new FluentFunctionException( functionName, this.getMessage(), this.getCause() );
    }

    /**
     * Wrap a Throwable into a FluentFunctionException, without altering the message.
     *
     * @param cause Throwable to wrap
     * @return FluentFunctionException
     */
    public static FluentFunctionException wrap(Throwable cause) {
        return new FluentFunctionException( null, cause.getMessage(), cause );
    }

    /**
     * Wrap a Throwable into a FluentFunctionException, with a custom message.
     *
     * @param cause Throwable to wrap
     * @return FluentFunctionException
     */
    public static FluentFunctionException wrap(Throwable cause, String formatString, Object... args) {
        return new FluentFunctionException( null, String.format( formatString, args ), cause );
    }

    /**
     * Create a FluentFunctionException with the given message.
     *
     * @param formatString Message format String, as per String.format()
     * @param args         (optional) arguments for the format string
     * @return FluentFunctionException
     */
    public static FluentFunctionException create(String formatString, Object... args) {
        return new FluentFunctionException( String.format( formatString, args ) );
    }
}
