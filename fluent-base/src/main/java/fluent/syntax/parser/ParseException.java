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

package fluent.syntax.parser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/// Exceptions encountered during parsing.
@NullMarked
public class ParseException extends RuntimeException {
    private final static String UNSPECIFIED = "[*ARGUMENT UNSPECIFIED*]";

    private final ErrorCode errorCode;
    private final String arg;               // arguments
    private final int line;                 // 1-based, like a text editor
    @Nullable private final String ch;      // input received; may be null

    private ParseException(ErrorCode errorCode, String arg, int line, @Nullable String ch) {
        super( Objects.requireNonNull(errorCode).message() );
        Objects.requireNonNull( arg );

        this.errorCode = errorCode;
        this.arg = arg;
        this.line = line;
        this.ch = ch;
    }

    public static ParseException of(ErrorCode errorCode, String arg, int line) {
        return new ParseException( errorCode, arg, line, null );
    }

    public static ParseException of(ErrorCode errorCode, String arg, int line, @Nullable String received) {
        return new ParseException( errorCode, arg, line, received );
    }

    public static ParseException of(ErrorCode errorCode, FTLStream stream) {
        return new ParseException(
                errorCode,
                UNSPECIFIED,
                stream.positionToLine(),
                FTLStream.toString( stream.at() )
        );
    }

    public static ParseException of(ErrorCode errorCode, String argument, FTLStream stream) {
        return new ParseException(
                errorCode,
                argument,
                stream.positionToLine(),
                FTLStream.toString( stream.at() )
        );
    }

    /** The error code of the Exception */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * Line of input on which the error occurred. The first line is '1' (like a text editor).
     * <p>
     *     NOTE: This can be '0' if the error occurred at the end of file (EOF)
     * </p>
     *
     */
    public int line() { return line; }

    /** True if error occurred at EOF */
    public boolean isEOF() { return (line == 0); }


    private String received() {
        if(ch == null) {
            return " ";
        } else {
            return " [received "+ch+"] ";
        }
    }

    @Override
    public String getMessage() {
        return  errorCode +
                (isEOF() ? " [at <EOF>" : " [line "+line) + ']' +
                received() +
                String.format( super.getMessage(), arg );
    }


    /// ParseException error codes.
    @NullMarked
    public enum ErrorCode {
        E0001( "Generic error" ),
        E0002( "Expected an entry start" ),
        E0003( "Expected token: '%s'" ),
        E0004( "Expected a character from range: '%s'" ),
        E0005( "Expected message '%s' to have a value and/or attributes" ),
        E0006( "Expected term '-%s' to have a value" ),
        E0007( "Keyword cannot end with a whitespace" ),
        E0008( "The callee ('%s') must be an upper-case identifier or a term" ),
        E0009( "The argument name has to be a simple identifier" ),
        E0010( "Expected one of the variants to be marked as default (*)" ),
        E0011( "Expected at least one variant after '->'" ),
        E0012( "Expected value" ),
        E0013( "Expected variant key" ),
        E0014( "Expected literal" ),
        E0015( "Only one variant can be marked as default (*)" ),
        E0016( "Message references cannot be used as selectors" ),
        E0017( "Terms cannot be used as selectors" ),
        E0018( "Attributes of messages cannot be used as selectors" ),
        E0019( "Attributes of terms cannot be used as placeables" ),
        E0020( "Unterminated string expression" ),
        E0021( "Positional arguments must not follow named arguments" ),
        E0022( "Named arguments must be unique" ),
        E0024( "Cannot access variants of a message." ),
        E0025( "Unknown escape sequence: '%s'" ),
        E0026( "Invalid Unicode escape sequence: '%s'" ),
        E0027( "Unbalanced closing brace in TextElement" ),
        E0028( "Expected an inline expression" ),
        E0029( "Expected simple expression as selector" ),
        E0030( "NumberLiteral out of range: '%s'" ),
        E0031( "Positional arguments in term '-%s'"),
        E0032( "The value of a named argument or option must be a String or Number literal");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }


    }
}
