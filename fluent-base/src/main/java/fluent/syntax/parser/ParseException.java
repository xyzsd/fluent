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

package fluent.syntax.parser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/// Exceptions encountered during parsing Fluent (FTL) resources.
@NullMarked
public class ParseException extends RuntimeException {

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

    private ParseException(Throwable cause) {
        super(ErrorCode.E0001.message, cause);
        this.errorCode = ErrorCode.E0001;
        this.arg = cause.getMessage();
        this.line = 0;  // hmmm
        this.ch = null;
    }

    ///  Create a 'generic' ParseException that wraps another exception.
    ///
    /// This should be used very sparingly.
    ///
    public static ParseException of(Throwable cause) {
        return new ParseException( cause );
    }
    /// Create a ParseException with an explicit message argument and line number.
    ///
    /// @param errorCode the error code describing the parsing failure
    /// @param arg the argument to interpolate into the error message format
    /// @param line the 1-based line where the error occurred; 0 indicates EOF
    /// @return a new ParseException instance
    public static ParseException of(ErrorCode errorCode, String arg, int line) {
        return new ParseException( errorCode, arg, line, null );
    }

    /// Create a ParseException with an explicit message argument, line number and the last received character.
    ///
    /// @param errorCode the error code describing the parsing failure
    /// @param arg the argument to interpolate into the error message format
    /// @param line the 1-based line where the error occurred; 0 indicates EOF
    /// @param received the last received character (as a string) from the input, or null if unknown
    /// @return a new ParseException instance
    public static ParseException of(ErrorCode errorCode, String arg, int line, @Nullable String received) {
        return new ParseException( errorCode, arg, line, received );
    }


    /// Returns the structured error code associated with this exception.
    ///
    /// @return the error code
    public ErrorCode errorCode() {
        return errorCode;
    }

    /// Line of input on which the error occurred.
    ///
    /// The first line is 1 (as in a typical text editor). A value of 0 indicates that
    /// the error occurred at the end of file (EOF).
    ///
    /// @return the 1-based line number, or 0 if at EOF
    public int line() { return line; }

    /// Returns whether the error occurred at end-of-file.
    ///
    /// @return true if [#line()] equals 0 (EOF), false otherwise
    public boolean isEOF() { return (line == 0); }


    /// Formats the optional received character for inclusion in the exception message.
    ///
    /// If no character was recorded, returns a single space to keep message spacing consistent.
    ///
    /// The 'character' may be a String, to better denote special characters (e.g., non-printable
    /// characters may be in hex).
    ///
    /// @return a formatted " \[received X\] " segment or a single space
    private String received() {
        if(ch == null) {
            return " ";
        } else {
            return " [received "+ch+"] ";
        }
    }

    /// Returns a rich, formatted error message composed of:
    /// - the [ErrorCode]
    /// - the location (line number or <EOF>)
    /// - the (optionally) received character
    /// - the formatted base message with the provided argument
    ///
    /// @return the fully formatted error message
    @Override
    public String getMessage() {
        return  errorCode +
                (isEOF() ? " [at <EOF>" : " [line "+line) + ']' +
                received() +
                String.format( super.getMessage(), arg );
    }


    /// ParseException error codes.
    ///
    /// Each code carries a human-readable message that may include a single
    /// `%s` placeholder for optional argument interpolation.
    ///
    /// The human-readable diagnostic String is not guaranteed to be stable between versions, as it is
    /// only intended for diagnostics/error messages.
    @NullMarked
    public enum ErrorCode {
        /// Generic catch‑all parsing error with contextual message.
        E0001( "Generic error: %s" ),
        /// Expected the beginning of an FTL entry (message, term, or comment).
        E0002( "Expected an entry start" ),
        /// A specific token was required at this position but was not found.
        E0003( "Expected token: %s" ),
        /// Expected a character within the indicated range or group (e.g., 0–9 or a–z).
        E0004( "Expected %s" ),
        /// A message declaration is missing its pattern and/or attributes.
        E0005( "Expected message '%s' to have a pattern and/or attributes" ),
        /// A term declaration is missing its required pattern.
        E0006( "Expected term '-%s' to have a pattern" ),
        /// Keywords must not end with whitespace.
        E0007( "Keyword cannot end with a whitespace" ),
        /// A function callee must be an upper‑case identifier or a term reference.
        E0008( "The callee ('%s') must be an upper-case identifier or a term" ),
        /// Argument names must be simple identifiers.
        E0009( "The argument name has to be a simple identifier" ),
        /// One of the variants in a select expression must be marked as the default (*).
        E0010( "Expected one of the variants to be marked as default (*)" ),
        /// A select expression must define at least one variant after '->'.
        E0011( "Expected at least one variant after '->'" ),
        /// A pattern was expected here but not found.
        E0012( "Expected pattern" ),
        /// A variant key was expected here but not found.
        E0013( "Expected variant key" ),
        /// A literal value (String or Number) was expected here.
        E0014( "Expected literal" ),
        /// Only one variant may be marked as the default (*).
        E0015( "Only one variant can be marked as default (*)" ),
        /// Message references cannot be used as select expression selectors.
        E0016( "Message references cannot be used as selectors" ),
        /// Terms cannot be used as select expression selectors.
        E0017( "Terms cannot be used as selectors" ),
        /// Message attributes cannot be used as select expression selectors.
        E0018( "Attributes of messages cannot be used as selectors" ),
        /// Term attributes cannot be used as placeables.
        E0019( "Attributes of terms cannot be used as placeables" ),
        /// A string expression was opened but not properly terminated.
        E0020( "Unterminated string expression" ),
        /// Positional arguments may not follow named arguments.
        E0021( "Positional arguments must not follow named arguments" ),
        /// Named arguments must be unique within a call.
        E0022( "Named arguments must be unique" ),
        /// Variants of a message are not directly accessible.
        E0024( "Cannot access variants of a message." ),
        /// Encountered an unknown escape sequence in a string.
        E0025( "Unknown escape sequence: '%s'" ),
        /// The Unicode escape sequence is malformed or out of range.
        E0026( "Invalid Unicode escape sequence: '%s'" ),
        /// Found a '}' without a matching opening '{' in text.
        E0027( "Unbalanced closing brace in TextElement" ),
        /// An inline expression was expected here.
        E0028( "Expected an inline expression" ),
        /// The selector of a select expression must be a simple expression.
        E0029( "Expected simple expression as selector" ),
        /// A numeric literal is outside the supported range.
        E0030( "NumberLiteral out of range: '%s'" ),
        /// Positional arguments are not allowed in term references.
        E0031( "Positional arguments in term '-%s'"),
        /// A named argument or option must use a String or Number literal as its pattern.
        E0032( "The pattern of a named argument or option must be a String or Number literal. String literals must be quoted. For example: timeStyle:\"long\"");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        /// Returns the raw (unformatted) message template for this error code.
        /// The template may include a single `%s` placeholder.
        ///
        /// @return the message template associated with this code
        public String message() {
            return message;
        }
    }
}
