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
 *  Portions of this project were inspired by or derived from the fluent-rs project at
 *  https://github.com/projectfluent/fluent-rs
 *
 */

package fluent.syntax.parser;

import fluent.bundle.FluentResource;
import fluent.syntax.AST.*;
import fluent.syntax.parser.ParseException.ErrorCode;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fluent.syntax.parser.FTLStream.*;

/// Parse Fluent Translation List (FTL) source into a FluentResource AST.
///
/// This parser consumes an FTLStream and produces a FluentResource that can be queried
/// for messages, terms, and metadata. Parse exceptions are also accumulated within the FluentResource.
///
/// There are two parsing modes:
/// - Simple: comments and junk nodes are ignored for a smaller, faster AST. Best for general use.
/// - Complete: comments are preserved and junk nodes are recorded when errors occur. Best for implementing
///   translation or localization tools.
///
/// Notes
/// - “Junk” nodes are ranges of source captured when a parse error is encountered. They are useful for diagnostics.
/// - Ignoring comments and junk yields a more memory‑efficient AST and can improve performance.
@NullMarked
public class FTLParser {
    ///  default initial size for attribute lists
    private static final int ATTRIBUTE_LIST_SIZE = 4;


    private FTLParser() {}

    /// Parse an FTLStream into a FluentResource using the simple mode.
    ///
    /// In simple mode:
    /// - Comments are ignored.
    /// - Junk nodes are not created, but parse errors are still collected on the resource.
    ///
    /// @param stream the input stream to parse
    /// @return the parsed FluentResource
    public static FluentResource parse(final FTLStream stream) {
        return parseSimple( stream );
    }

    /// Parse an FTLStream into a FluentResource with control over comment and junk handling.
    ///
    /// When `ignoreCommentsAndJunk` is true:
    /// - Comments are not included in the AST.
    /// - Junk nodes are not created, though parse errors are still collected.
    /// When false, comments are preserved and junk nodes are created on parse errors.
    ///
    /// @param stream                the input stream to parse
    /// @param ignoreCommentsAndJunk if true, omit Comment and Junk nodes from the AST
    /// @return the parsed FluentResource
    public static FluentResource parse(final FTLStream stream, final boolean ignoreCommentsAndJunk) {
        return (ignoreCommentsAndJunk ? parseSimple( stream ) : parseComplete( stream ));
    }

    private static FluentResource parseSimple(final FTLStream ps) {
        List<Entry> entries = new ArrayList<>();
        List<ParseException> errors = new ArrayList<>();

        ps.skipBlankBlockNLC();

        while (ps.hasRemaining()) {
            final int entryStart = ps.position();

            try {
                Entry entry = getEntry( ps, entryStart, true );
                if (entry != null) {
                    entries.add( entry );
                }
            } catch (ParseException ex) {
                ps.skipToNextEntryStart();  // should not throw!
                errors.add( ex );
                // 'Junk' is ignored
            }
            ps.skipBlankBlockNLC();
        }

        return new FluentResource( entries, errors, List.of() );
    }


    private static FluentResource parseComplete(final FTLStream ps) {
        List<Entry> entries = new ArrayList<>();
        List<Junk> junk = new ArrayList<>();
        List<ParseException> errors = new ArrayList<>();
        ps.skipBlankBlockNLC();

        Commentary.Comment lastComment = null;
        int lastBlankCount = 0;

        while (ps.hasRemaining()) {
            final int entryStart = ps.position();
            try {

                Entry entry = getEntry( ps, entryStart, false );
                assert entry != null;   // always true when ignoreComments is false!

                if (lastComment != null) {
                    final Commentary.Comment comment = lastComment;
                    lastComment = null;

                    if ((entry instanceof Message msg) && (lastBlankCount < 2)) {
                        entry = msg.withComment( comment );
                    } else if ((entry instanceof Term term) && (lastBlankCount < 2)) {
                        entry = term.withComment( comment );
                    } else {
                        entries.add( comment );
                    }
                }

                if (entry instanceof Commentary.Comment comment) {
                    lastComment = comment;
                } else {
                    entries.add( entry );
                }
            } catch (ParseException ex) {
                ps.skipToNextEntryStart();  // should not throw
                errors.add( ex );
                final String text = ps.subString( entryStart, ps.position() );
                junk.add( new Junk( text ) );
            }
            lastBlankCount = ps.skipBlankBlock();
        }

        if (lastComment != null) {
            entries.add( lastComment );
        }

        return new FluentResource( entries, errors, junk );
    }


    // Only null when getComment() returns null
    @Nullable
    private static Entry getEntry(final FTLStream ps, final int entryStart, final boolean ignoreComments) {
        return switch (ps.at()) {
            case '#' -> getComment( ps, ignoreComments );
            case '-' -> getTerm( ps, entryStart );
            default -> getMessage( ps, entryStart );
        };
    }


    private static Message getMessage(final FTLStream ps, final int entryStart) {
        final Identifier id = getIdentifier( ps );

        ps.skipBlankInline();
        ps.expectChar( (byte) '=' );

        // remember, pattern can be empty (null) ... if an attribute is present.
        final Pattern pattern = FTLPatternParser.getPattern( ps );

        ps.skipBlankBlockNLC();

        // and attributes can be empty
        final List<Attribute> attributes = getAttributes( ps );

        // but attributes and pattern cannot BOTH be empty.
        // use entryStart as position (more helpful)
        if (pattern == null && attributes.isEmpty()) {
            throw ParseException.of(
                    ErrorCode.E0005,
                    id.name(),
                    ps.positionToLine( entryStart )
            );
        }

        return new Message( id, pattern, attributes, null );
    }

    private static Term getTerm(final FTLStream ps, final int entryStart) {
        ps.expectChar( (byte) '-' );
        final Identifier id = getIdentifier( ps );

        ps.skipBlankInline();
        ps.expectChar( (byte) '=' );
        ps.skipBlankInline();

        final Pattern pattern = FTLPatternParser.getPattern( ps );
        ps.skipBlankBlockNLC();

        final List<Attribute> attributes = getAttributes( ps );

        if (pattern != null) {
            return new Term( id, pattern, attributes );
        } else {
            throw ParseException.of(
                    ErrorCode.E0006,
                    id.name(),
                    ps.positionToLine( entryStart ) // safe if entryStart OOB
            );
        }
    }


    private static List<Attribute> getAttributes(final FTLStream ps) {
        List<Attribute> attributes = new ArrayList<>( ATTRIBUTE_LIST_SIZE );

        while (ps.hasRemaining()) {
            final int line_start = ps.position();
            ps.skipBlankInline();

            if (!ps.hasRemaining()) {
                break;  // early EOF
            }

            if (!ps.isCurrentChar( (byte) '.' )) {
                ps.position( line_start );
                break;
            }

            attributes.add( getAttribute( ps ) );
        }
        return attributes;
    }


    private static Attribute getAttribute(final FTLStream ps) {
        ps.expectChar( (byte) '.' );
        final Identifier id = getIdentifier( ps );
        ps.skipBlankInline();
        ps.expectChar( (byte) '=' );
        final Pattern pattern = FTLPatternParser.getPattern( ps );

        if (pattern == null) {
            // Attributes must have a Pattern.
            // If we want to be lenient, we could return null here, and the attribute will be ignored.
            throw parseException( ErrorCode.E0012, ps );
        } else {
            return new Attribute( id, pattern );
        }
    }

    private static Identifier getIdentifier(final FTLStream ps) {
        // new version
        final int idStart = ps.position();
        final int idEnd = ps.getIdentifierEnd( idStart );

        // initial character of identifier invalid
        if (idStart == idEnd) {
            throw parseException( ErrorCode.E0004, "character from range [a-zA-Z] for the start of an identifier", ps );
        }

        final String idName = ps.subString( idStart, idEnd );
        ps.position( idEnd );
        return new Identifier( idName );
    }


    private static Optional<Identifier> getAttributeAccessor(final FTLStream ps) {
        if (!ps.takeCharIf( (byte) '.' )) {
            return Optional.empty();
        } else {
            return Optional.of( getIdentifier( ps ) );
        }
    }

    private static VariantKey getVariantKey(final FTLStream ps) {
        if (!ps.takeCharIf( (byte) '[' )) {
            throw parseException( ErrorCode.E0003, "[", ps );
        }
        ps.skipBlank();

        final VariantKey variantKey;
        if (ps.hasRemaining()) {
            if (ps.isNumberStart()) {
                variantKey = getNumberLiteral( ps );
            } else {
                variantKey = getIdentifier( (ps) );
            }
        } else {
            // a variant key is expected!
            throw parseException( ErrorCode.E0013, ps );
        }

        ps.skipBlank();
        ps.expectChar( (byte) ']' );

        return variantKey;
    }

    private static List<Variant> getVariants(final FTLStream ps) {
        List<Variant> variants = new ArrayList<>( 4 );
        boolean hasDefault = false;

        while (ps.hasRemaining() && (ps.isCurrentChar( (byte) '*' ) || ps.isCurrentChar( (byte) '[' ))) {
            final boolean isDefault = ps.takeCharIf( (byte) '*' );

            if (isDefault) {
                if (hasDefault) {
                    throw parseException(
                            ErrorCode.E0015,
                            ps
                    );
                } else {
                    hasDefault = true;
                }
            }

            final VariantKey key = getVariantKey( ps );
            final Pattern value = FTLPatternParser.getPattern( ps );
            if (value == null) {
                throw parseException( ErrorCode.E0012, ps );
            }

            variants.add( new Variant( key, value, isDefault ) );
            ps.skipBlank();
        }

        if (variants.isEmpty()) {
            throw parseException( ErrorCode.E0011, ps );
        } else if (!hasDefault) {
            throw parseException( ErrorCode.E0010, ps );
        }

        return variants;
    }


    // This can return null -- but ONLY when comments are disabled
    @Nullable
    private static Commentary getComment(final FTLStream ps, final boolean ignoreComments) {
        if (ignoreComments) {
            ps.skipToEOL();
            return null;
        }
        return FTLCommentParser.getComment( ps );
    }


    // also used by FTLPatternParser_2
    static Expression getPlaceable(final FTLStream ps) {
        ps.expectChar( (byte) '{' );
        ps.skipBlank();

        final Expression exp = getExpression( ps );
        ps.skipBlankInline();
        ps.expectChar( (byte) '}' );
        if (exp instanceof InlineExpression.TermReference termReference) {
            if (termReference.attributeID() != null) {
                throw parseException( ErrorCode.E0019, ps );
            }
        }

        return exp;
    }

    // InlineExpression is bound; can return Expression
    private static Expression getExpression(final FTLStream ps) {
        final InlineExpression exp = getInlineExpression( ps );

        // For InlineExpressions:
        //      Permitted:
        //          Literals, FunctionReferences, VariableReferences
        //      Never Permitted:
        //          Placeables, MessageReference, TermReferences

        ps.skipBlank();

        if (!ps.isCurrentChar( (byte) '-' ) || !ps.isNextChar( (byte) '>' )) {
            if (exp instanceof InlineExpression.TermReference ref) {
                if (ref.attributeID() != null) {
                    throw parseException( ErrorCode.E0019, ps );
                }
            }
            return exp;
        }

        switch (exp) {
            case InlineExpression.MessageReference ref -> {
                if (ref.attributeID() == null) {
                    throw parseException( ErrorCode.E0016, ps );
                } else {
                    throw parseException( ErrorCode.E0018, ps );
                }
            }
            case InlineExpression.TermReference ref -> {
                if (ref.attributeID() == null) {
                    throw parseException( ErrorCode.E0017, ps );
                }
            }
            case PatternElement.Placeable _ -> throw parseException( ErrorCode.E0029, ps );
            default -> { /* do nothing */ }
        }

        // skip over the '->' in selector (originally this was just a ps.inc(2))
        // caveat: because we check each individually, this could result in a confusing error message
        ps.expectChar( (byte) '-' );
        ps.expectChar( (byte) '>' );

        ps.skipBlankInline();
        if (!ps.hasRemaining() || !ps.skipEOL()) {
            throw parseException( ErrorCode.E0004, "either '\\n' (LF) or '\\r\\n' (CRLF)", ps );
        }
        ps.skipBlank();
        return SelectExpression.of( exp, getVariants( ps ) );
    }


    private static InlineExpression getInlineExpression(final FTLStream ps) {
        final byte initialChar = ps.at();
        if (initialChar == '"') {
            ps.inc();
            int sliceStart = ps.position();
            StringBuilder sb = new StringBuilder( 64 );     // todo: sizing
            while (ps.hasRemaining()) {
                final byte ch = ps.at();
                if (ch == '\\') {
                    if (sliceStart != -1) {
                        sb.append( ps.subString( sliceStart, ps.position() ) );   // append non-escaped text, if any
                    }
                    sliceStart = -1;
                    switch (ps.at( ps.position() + 1 )) {
                        // special chars, within BASIC LATIN range. No need to decode for these common chars
                        case '\\' -> {
                            sb.append( '\\' );
                            ps.inc( 2 );
                        }
                        case '{' -> {
                            sb.append( '{' );
                            ps.inc( 2 );
                        }
                        case '"' -> {
                            sb.append( '"' );
                            ps.inc( 2 );
                        }
                        case 'u' -> {
                            ps.inc( 2 );   // char after 'u', then Unicode character in the U+0000 to U+FFFF
                            sb.appendCodePoint( ps.getUnicodeEscape( 4 ) );
                        }
                        case 'U' -> {
                            ps.inc( 2 );   // char after 'U', then any Unicode character
                            sb.appendCodePoint( ps.getUnicodeEscape( 6 ) );
                        }
                        default -> throw parseException(
                                ErrorCode.E0025,
                                "\\" + FTLStream.byteToString( ps.at( ps.position() + 1 ) ),
                                ps
                        );
                    }
                } else if (ch == '"') {
                    if (sliceStart != -1) {
                        sb.append( ps.subString( sliceStart, ps.position() ) );
                    }
                    break;
                } else if (ch == '\n') {
                    throw parseException( ErrorCode.E0020, ps );
                } else {
                    sliceStart = (sliceStart == -1) ? ps.position() : sliceStart;
                    ps.inc();
                }
            }

            ps.expectChar( (byte) '"' );
            return Literal.StringLiteral.of( sb.toString() );
        } else if (isASCIIDigit( initialChar )) {
            return getNumberLiteral( ps );
        } else if (initialChar == '-') {
            ps.inc();
            if (ps.isIdentifierStart()) {
                final Identifier identifier = getIdentifier( ps );
                final Identifier attribAccessorID = getAttributeAccessor( ps ).orElse( null );
                final CallArguments callArguments = getCallArguments( ps ).orElse( null );

                // exception if there are positionals arguments (which are not allowed) in TermReferences
                validateTermCallArguments( ps, identifier, callArguments );

                // Get NamedArguments (noting that CallArguments could be null).
                final List<NamedArgument> namedArgs = (callArguments == null) ? List.of() : callArguments.named();

                return new InlineExpression.TermReference(
                        identifier,
                        attribAccessorID,
                        namedArgs
                );
            } else {
                ps.dec();
                return getNumberLiteral( ps );
            }
        } else if (initialChar == '$') {
            ps.inc();
            return new InlineExpression.VariableReference( getIdentifier( ps ) );
        } else if (isASCIIAlphabetic( initialChar )) {
            // identifier could be a MessageReference or a FunctionReference. So the identifier
            // structure has to be refined.
            final Identifier id = getIdentifier( ps );

            //final Identifier id = getIdentifier( ps );
            return getCallArguments( ps )
                    .<InlineExpression>map( callArgs -> {
                        validateFunctionName( id, ps );
                        return new InlineExpression.FunctionReference( id, callArgs );
                    } )
                    .orElseGet( () -> new InlineExpression.MessageReference( id,
                            getAttributeAccessor( ps ).orElse( null ) )
                    );
        } else if (initialChar == '{') {
            return new PatternElement.Placeable( getPlaceable( ps ) );
        } else {
            throw parseException( ErrorCode.E0028, ps );
        }
    }

    // terms with CallArguments should not have positionals arguments.
    private static void validateTermCallArguments(final FTLStream ps, final Identifier id, @Nullable final CallArguments in) {
        if (in != null && !in.positionals().isEmpty()) {
            throw parseException( ErrorCode.E0031, id.name(), ps );
        }
    }

    // get call arguments for FTL Functions
    private static Optional<CallArguments> getCallArguments(final FTLStream ps) {
        ps.skipBlank();
        if (!ps.takeCharIf( (byte) '(' )) {
            return Optional.empty();
        }

        List<Expression> positional = new ArrayList<>();
        List<NamedArgument> named = new ArrayList<>();
        List<String> argNames = new ArrayList<>();          // or Set<>... but there are likely few entries

        ps.skipBlank();
        while (ps.hasRemaining()) {
            if (ps.isCurrentChar( (byte) ')' )) {
                break;
            }

            final InlineExpression expr = getInlineExpression( ps );

            if (expr instanceof InlineExpression.MessageReference msgRef) {
                ps.skipBlank();
                if (ps.isCurrentChar( (byte) ':' )) {
                    if (argNames.contains( msgRef.name() )) {
                        throw parseException( ErrorCode.E0022, ps );
                    }
                    ps.inc();
                    ps.skipBlank();

                    final InlineExpression value = getInlineExpression( ps );
                    if (value instanceof Literal<?> literal) {
                        argNames.add( msgRef.name() );
                        named.add( new NamedArgument( new Identifier( msgRef.name() ), literal ) );
                    } else {
                        throw parseException( ErrorCode.E0032, ps );
                    }
                } else {
                    if (!argNames.isEmpty()) {
                        throw parseException( ErrorCode.E0021, ps );
                    }
                    positional.add( expr );
                }
            } else {
                if (!argNames.isEmpty()) {
                    throw parseException( ErrorCode.E0021, ps );
                }
                positional.add( expr );
            }

            ps.skipBlank();
            ps.takeCharIf( (byte) ',' );
            ps.skipBlank();
        }

        ps.expectChar( (byte) ')' );
        return Optional.of( new CallArguments( positional, named ) );
    }


    private static Literal.NumberLiteral<?> getNumberLiteral(final FTLStream ps) {
        final int start = ps.position();
        ps.takeCharIf( (byte) '-' );
        ps.skipDigits();
        if (ps.takeCharIf( (byte) '.' )) {
            ps.skipDigits();
        }

        final String literalString = ps.subString( start, ps.position() );
        try {
            return Literal.NumberLiteral.from( literalString );
        } catch (NumberFormatException e) {
            throw parseException( ErrorCode.E0030, literalString, ps );
        }
    }


    // We found an identifier, but, function names are not allowed to have lower-cased letters.
    // throws if not valid function identifier (uppercase+special ('-', '_'). **empty is valid**
    private static void validateFunctionName(final Identifier identifier, final FTLStream ps) {
        final String name = identifier.name();
        // this works b/c we are working with ASCII (all identifiers characters are ASCII)
        // we only want to see if there are lowercase letters present; if so, flag it.
        // CONSIDER: this could be vectorized but may not be worth it. profile results.

        for (int i = 0; i < name.length(); i++) {
            final byte b = (byte) name.charAt( i );
            if (FTLStream.isASCIILowerCase( b )) {
                throw ParseException.of(
                        ErrorCode.E0008,
                        name,
                        ps.positionToLine( ps.position() ),
                        FTLStream.byteToString( b )
                );
            }
        }
    }


    /// Create a ParseException from the current position of an [FTLStream].
    /// The message argument is left unspecified.
    ///
    /// These methods should be used instead of ParseException.of(), because these methods are
    /// tolerant of potential out-of-bound positions.
    ///
    /// @param errorCode the error code describing the parsing failure
    /// @param stream    the token stream that supplies current location and received byte
    /// @return a new ParseException instance pointing at the stream's current line
    static ParseException parseException(final ErrorCode errorCode, final FTLStream stream) {
        return parseException( errorCode, "[argument unspecified]", stream );
    }

    /// Create a ParseException from the current position of an [FTLStream]
    /// with an explicit message argument.
    ///
    /// These methods should be used instead of ParseException.of(), because these methods are
    /// tolerant of potential out-of-bound positions.
    ///
    /// @param errorCode the error code describing the parsing failure
    /// @param argument  the argument to interpolate into the error message format
    /// @param stream    the token stream that supplies current location and received byte
    /// @return a new ParseException instance pointing at the stream's current line
    static ParseException parseException(final ErrorCode errorCode, final String argument, final FTLStream stream) {
        // if we are EOF, stream.at() will throw an exception (if there is no padding)
        final int line = stream.positionToLine( stream.position() );
        final String received = (line != 0) ? byteToString( stream.at() ) : byteToString( EOF );

        return ParseException.of(
                errorCode,
                argument,
                line,
                received
        );
    }
}
