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
 *  Portions of this project were inspired by or derived from the fluent-rs project at
 *  https://github.com/projectfluent/fluent-rs
 *
 */

package fluent.syntax.parser;

import fluent.bundle.FluentResource;
import fluent.syntax.AST.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fluent.syntax.parser.FTLStream.isASCIIAlphabetic;
import static fluent.syntax.parser.FTLStream.isASCIIDigit;

/**
 * Parse a FTLStream into a FluentResource, which can then be queried as needed.
 * <p>
 * Comments can be included within the FluentResource AST, or, ignored. If comments are ignored, 'junk' is also
 * ignored. 'Junk' nodes are created during parse errors and can be useful for debugging.
 * </p>
 * <p>
 * Ignoring Comments (and Junk) makes for a smaller, more memory-efficient AST and can improve parsing performance.
 * </p>
 */
@NullMarked
public class FTLParser {


    /**
     * Parse an FTLStream into a FluentResource.
     * <p>
     * Ignores comments, and does not create 'Junk' nodes by default.
     * Parse errors are still reported in the FluentResource as encountered.
     *
     * @param stream stream to parse
     * @return FluentResource
     */
    public static FluentResource parse(final FTLStream stream) {
        return parseSimple( stream );
    }


    /**
     * Parse an FTLStream into a FluentResource.
     * <p>
     * Optionally ignores comments and does not create 'Junk' nodes depending upon {@code ignoreCommentsAndJunk}.
     * Parse errors are reported in the FluentResource as encountered.
     *
     * @param stream                stream to parse
     * @param ignoreCommentsAndJunk if true, do not create Comment or Junk AST nodes
     * @return FluentResource
     */
    public static FluentResource parse(final FTLStream stream, final boolean ignoreCommentsAndJunk) {
        return (ignoreCommentsAndJunk ? parseSimple( stream ) : parseComplete( stream ));
    }


    private FTLParser() {}


    private static FluentResource parseSimple(final FTLStream ps) {
        List<Entry> entries = new ArrayList<>();
        List<ParseException> errors = new ArrayList<>();

        ps.skipBlankBlock();

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
            ps.skipBlankBlock();
        }

        return new FluentResource( entries, errors, List.of() );
    }


    private static FluentResource parseComplete(final FTLStream ps) {
        List<Entry> entries = new ArrayList<>();
        List<Junk> junk = new ArrayList<>();
        List<ParseException> errors = new ArrayList<>();
        ps.skipBlankBlock();

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
        ps.expectChar( '=' );

        // remember, pattern can be empty...
        final Optional<Pattern> pattern = FTLPatternParser.getPattern( ps );

        ps.skipBlankBlock();

        // and attributes can be empty
        final List<Attribute> attributes = getAttributes( ps );

        // but attributes and pattern cannot BOTH be empty
        if (pattern.isEmpty() && attributes.isEmpty()) {
            throw ParseException.of(
                    ParseException.ErrorCode.E0005,
                    id.name(),
                    ps.positionToLine( entryStart )
            );
        }

        return new Message( id, pattern.orElse( null ),
                attributes, null );
    }

    private static Term getTerm(final FTLStream ps, final int entryStart) {
        ps.expectChar( '-' );
        final Identifier id = getIdentifier( ps );
        ps.skipBlankInline();
        ps.expectChar( '=' );
        ps.skipBlankInline();

        final Optional<Pattern> value = FTLPatternParser.getPattern( ps );

        ps.skipBlankBlock();

        final List<Attribute> attributes = getAttributes( ps );

        return value.map( v -> new Term( id, v, attributes ) )
                .orElseThrow( () -> ParseException.of(
                        ParseException.ErrorCode.E0006,
                        id.name(),
                        ps.positionToLine( entryStart )
                ) );
    }


    // TODO: this suppresses errors if get_attribute fails; give warning? store error somewhere?
    //       re-evaluate and create a test for this
    private static List<Attribute> getAttributes(final FTLStream ps) {
        List<Attribute> attributes = new ArrayList<>();

        while (ps.hasRemaining()) {
            final int line_start = ps.position();
            ps.skipBlankInline();
            if (!ps.hasRemaining()) {
                // TODO: this may be an error condition (early EOF or invalid entry)
                //       validate and if so generate error
                break;
            }

            if (!ps.isCurrentChar( '.' )) {
                ps.position( line_start );
                break;
            }

            if (getAttribute( ps ).map( attributes::add ).isEmpty()) {
                ps.position( line_start );
                break;
            }
        }
        return attributes;
    }


    private static Optional<Attribute> getAttribute(final FTLStream ps) {
        ps.expectChar( '.' );
        final Identifier id = getIdentifier( ps );
        ps.skipBlankInline();
        ps.expectChar( '=' );
        return FTLPatternParser.getPattern( ps )
                .map( pattern -> new Attribute( id, pattern ) );
        //.orElseThrow( () -> new ParseException( ParseException.ErrorCode.E0012 ));
    }

    // todo : hasRemaining() check on first if() or at peekPos
    private static Identifier getIdentifier(final FTLStream ps) {
        int peekPos = ps.position();
        if (isASCIIAlphabetic( ps.at( peekPos ) )) {
            peekPos += 1;
        } else {
            throw ParseException.of(
                    ParseException.ErrorCode.E0004,
                    "a-zA-Z",
                    ps.positionToLine( peekPos ),
                    FTLStream.toString( ps.at( peekPos ) )
            );
        }

        while (ps.hasRemaining()) {
            final char ch = ps.at( peekPos );
            if (isASCIIAlphabetic( ch ) || isASCIIDigit( ch ) || ch == '_' || ch == '-') {
                peekPos += 1;
            } else {
                break;
            }
        }

        String name = ps.subString( ps.position(), peekPos );
        ps.position( peekPos );
        return new Identifier( name );
    }


    private static Optional<Identifier> getAttributeAccessor(final FTLStream ps) {
        if (!ps.takeCharIf( '.' )) {
            return Optional.empty();
        } else {
            return Optional.of( getIdentifier( ps ) );
        }
    }

    private static VariantKey getVariantKey(final FTLStream ps) {
        if (!ps.takeCharIf( '[' )) {
            throw ParseException.of(
                    ParseException.ErrorCode.E0003,
                    "[",
                    ps
            );
        }
        ps.skipBlank();

        VariantKey variantKey;
        if (ps.isNumberStart()) {
            variantKey = getNumberLiteral( ps );
        } else {
            variantKey = getIdentifier( (ps) );
        }

        ps.skipBlank();
        ps.expectChar( ']' );

        return variantKey;
    }

    private static List<Variant> getVariants(final FTLStream ps) {
        List<Variant> variants = new ArrayList<>( 4 );
        boolean hasDefault = false;

        while (ps.isCurrentChar( '*' ) || ps.isCurrentChar( '[' )) {
            final boolean isDefault = ps.takeCharIf( '*' );

            if (isDefault) {
                if (hasDefault) {
                    throw ParseException.of(
                            ParseException.ErrorCode.E0015,
                            ps
                    );
                } else {
                    hasDefault = true;
                }
            }

            final VariantKey key = getVariantKey( ps );
            final Pattern value = FTLPatternParser.getPattern( ps )
                    .orElseThrow( () -> ParseException.of( ParseException.ErrorCode.E0012, ps ) );

            variants.add( new Variant( key, value, isDefault ) );
            ps.skipBlank();
        }

        if (!hasDefault) {
            throw ParseException.of(
                    ParseException.ErrorCode.E0010,
                    ps
            );
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


    // also used by FTLPatternParser
    static Expression getPlaceable(final FTLStream ps) {
        ps.expectChar( '{' );
        ps.skipBlank();

        final Expression exp = getExpression( ps );
        ps.skipBlankInline();
        ps.expectChar( '}' );
        if (exp instanceof InlineExpression.TermReference termReference) {
            if (termReference.attributeID() != null) {
                throw ParseException.of( ParseException.ErrorCode.E0019, ps );
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

        if (!ps.isCurrentChar( '-' ) || !ps.isNextChar( '>' )) {
            if (exp instanceof InlineExpression.TermReference ref) {
                if (ref.attributeID() != null) {
                    throw ParseException.of( ParseException.ErrorCode.E0019, ps );
                }
            }
            return exp;
        }

        switch (exp) {
            case InlineExpression.MessageReference ref -> {
                if (ref.attributeID() == null) {
                    throw ParseException.of( ParseException.ErrorCode.E0016, ps );
                } else {
                    throw ParseException.of( ParseException.ErrorCode.E0018, ps );
                }
            }
            case InlineExpression.TermReference ref -> {
                if (ref.attributeID() == null) {
                    throw ParseException.of( ParseException.ErrorCode.E0017, ps );
                }
            }
            case PatternElement.Placeable _ -> throw ParseException.of( ParseException.ErrorCode.E0029, ps );
            default -> { /* do nothing */ }
        }

        // skip over the '->' in selector (originally this was just a ps.inc(2))
        // caveat: because we check each individually, this could result in a confusing error message
        ps.expectChar( '-' );
        ps.expectChar( '>' );

        ps.skipBlankInline();
        if (!ps.skipEOL()) {
            throw ParseException.of( ParseException.ErrorCode.E0004, "'\\n' or '\\r\\n'", ps );
        }
        ps.skipBlank();
        return new SelectExpression( exp, getVariants( ps ) );
    }


    private static InlineExpression getInlineExpression(final FTLStream ps) {
        final char initialChar = ps.at();
        if (initialChar == '"') {
            ps.inc();
            int sliceStart = ps.position();
            StringBuilder sb = new StringBuilder( 64 );     // todo: sizing
            while (ps.hasRemaining()) {
                final char ch = ps.at();
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
                            ps.inc( 2 );   // char after 'u'
                            sb.appendCodePoint( ps.getUnicodeEscape( 4 ) );
                        }
                        case 'U' -> {
                            ps.inc( 2 );   // char after 'U'
                            sb.appendCodePoint( ps.getUnicodeEscape( 6 ) );
                        }
                        default -> throw ParseException.of(
                                ParseException.ErrorCode.E0025,
                                "\\" + FTLStream.toString( ps.at( ps.position() + 1 ) ),
                                ps
                        );
                    }
                } else if (ch == '"') {
                    if (sliceStart != -1) {
                        sb.append( ps.subString( sliceStart, ps.position() ) );
                    }
                    break;
                } else if (ch == '\n') {
                    throw ParseException.of( ParseException.ErrorCode.E0020, ps );
                } else {
                    sliceStart = (sliceStart == -1) ? ps.position() : sliceStart;
                    ps.inc();
                }
            }

            ps.expectChar( '"' );
            return Literal.StringLiteral.of( sb.toString() );
        } else if (isASCIIDigit( initialChar )) {
            return getNumberLiteral( ps );
        } else if (initialChar == '-') {
            ps.inc();
            if (ps.isIdentifierStart()) {
                final Identifier identifier = getIdentifier( ps );
                final Identifier attribAccessorID = getAttributeAccessor( ps ).orElse( null );
                final CallArguments callArguments = getCallArguments( ps ).orElse( null );

                //  create exception if there are positionals arguments in TermReferences
                validateTermCallArguments( ps, identifier, callArguments );

                return new InlineExpression.TermReference(
                        identifier,
                        attribAccessorID,
                        callArguments
                );
            } else {
                ps.dec();
                return getNumberLiteral( ps );
            }
        } else if (initialChar == '$') {
            ps.inc();
            return new InlineExpression.VariableReference( getIdentifier( ps ) );
        } else if (isASCIIAlphabetic( initialChar )) {
            final Identifier id = getIdentifier( ps );
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
            throw ParseException.of( ParseException.ErrorCode.E0028, ps );
        }
    }

    // terms with CallArguments should not have positionals arguments.
    private static void validateTermCallArguments(final FTLStream ps, final Identifier id, @Nullable final CallArguments in) {
        if (in != null && !in.positionals().isEmpty()) {
            throw ParseException.of( ParseException.ErrorCode.E0031, id.name(), ps );
        }
    }

    // get call arguments for FTL Functions
    private static Optional<CallArguments> getCallArguments(final FTLStream ps) {
        ps.skipBlank();
        if (!ps.takeCharIf( '(' )) {
            return Optional.empty();
        }

        List<Expression> positional = new ArrayList<>();
        List<NamedArgument> named = new ArrayList<>();
        List<String> argNames = new ArrayList<>();          // or Set<>... but there are likely few entries

        ps.skipBlank();
        while (ps.hasRemaining()) {
            if (ps.isCurrentChar( ')' )) {
                break;
            }

            final InlineExpression expr = getInlineExpression( ps );

            if (expr instanceof InlineExpression.MessageReference msgRef) {
                ps.skipBlank();
                if (ps.isCurrentChar( ':' )) {
                    if (argNames.contains( msgRef.name() )) {
                        throw ParseException.of( ParseException.ErrorCode.E0022, ps );
                    }
                    ps.inc();
                    ps.skipBlank();

                    final InlineExpression value = getInlineExpression( ps );
                    if (value instanceof Literal<?> literal) {
                        argNames.add( msgRef.name() );
                        named.add( new NamedArgument( new Identifier( msgRef.name() ), literal ) );
                    } else {
                        throw ParseException.of( ParseException.ErrorCode.E0032, ps );
                    }
                } else {
                    if (!argNames.isEmpty()) {
                        throw ParseException.of( ParseException.ErrorCode.E0021, ps );
                    }
                    positional.add( expr );
                }
            } else {
                if (!argNames.isEmpty()) {
                    throw ParseException.of( ParseException.ErrorCode.E0021, ps );
                }
                positional.add( expr );
            }

            ps.skipBlank();
            ps.takeCharIf( ',' );
            ps.skipBlank();
        }

        ps.expectChar( ')' );
        return Optional.of( new CallArguments( positional, named ) );
    }


    private static Literal.NumberLiteral<?> getNumberLiteral(final FTLStream ps) {
        final int start = ps.position();
        ps.takeCharIf( '-' );
        ps.skipDigits();
        if (ps.takeCharIf( '.' )) {
            ps.skipDigits();
        }

        final String literalString = ps.subString( start, ps.position() );
        try {
            return Literal.NumberLiteral.from( literalString );
        } catch (NumberFormatException e) {
            throw ParseException.of( ParseException.ErrorCode.E0030, literalString, ps );
        }
    }


    // throws if not valid function identifier (uppercase+special ('-', '_'). **empty is valid**
    private static void validateFunctionName(final Identifier identifier, final FTLStream ps) {
        final String name = identifier.name();
        for (int i = 0; i < name.length(); i++) {
            if (!FTLStream.isValidFnChar( name.charAt( i ) )) {
                throw ParseException.of(
                        ParseException.ErrorCode.E0008,
                        name,
                        ps.positionToLine(),
                        FTLStream.toString( name.charAt( i ) )
                );
            }
        }
    }


}
