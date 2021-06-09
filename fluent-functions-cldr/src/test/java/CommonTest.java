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

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import fluent.functions.FluentFunctionFactory;
import fluent.functions.Options;
import fluent.syntax.AST.Pattern;
import fluent.syntax.parser.FTLParser;
import fluent.syntax.parser.FTLStream;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Common methods for testing. These methods
// use a FluentBundle, but with excessive error checking
// to more easily use the methods. but will need zero-arg constructor..
public class CommonTest {

    private final FluentFunctionFactory fnFactory;
    private final Locale locale;
    private final boolean log;

    private CommonTest(FluentFunctionFactory factory, Locale locale, boolean log) {
        this.fnFactory = factory;
        this.locale = locale;
        this.log = log;
    }


    public static CommonTest init(FluentFunctionFactory factory, Locale locale, boolean log) {
        Objects.requireNonNull( factory );
        Objects.requireNonNull( locale );
        return new CommonTest( factory, locale, log );
    }


    public FluentBundle parse(String in) {
        return parse(in,Options.EMPTY);
    }

    public FluentBundle parse(String in, Options opts) {
        final FluentResource parse = FTLParser.parse( FTLStream.of( in ) );
        if(parse.hasErrors()) {
            System.err.println("**Errors on parse: "+parse.errors());
        }

        return FluentBundle.builder( locale, fnFactory )
                .addResource( parse )
                .setGlobalOptions( opts )
                .build();
    }

    public String msg(String in, String id) {
        return msg( in, id, Map.of() );
    }


    public String msg(String in, String id, Map<String, ?> args) {
        return msg(in, id, args, Options.EMPTY);
    }

    public String msg(String in, String id, Map<String, ?> args, Options opts) {
        FluentBundle bundle = parse( in, opts );
        final Optional<Pattern> messagePattern = bundle.getMessagePattern( id );
        if (messagePattern.isEmpty()) {
            return "**no pattern for message: " + id;
        }
        Pattern pattern = messagePattern.get();

        List<Exception> errors = new ArrayList<>();
        String result = bundle.formatPattern( pattern, args , errors);
        printMsg(result, errors);
        return result;
    }

    public String msg(String in, String msgID, String attribID) {
        assertNotNull( msgID );
        assertNotNull( attribID );
        FluentBundle bundle = parse( in );

        final Optional<Pattern> optionalPattern = bundle.getAttributePattern( msgID, attribID );
        assertTrue( optionalPattern.isPresent(),
                String.format( "Could not find message '%s' attribute '%s'" ,
                msgID, attribID));

        final Pattern pattern = optionalPattern.get();

        List<Exception> errors = new ArrayList<>();
        String result =  bundle.formatPattern( pattern, Map.of() , errors);
        printMsg(result, errors);
        return result;

    }


    private void printMsg(String result, List<Exception> errors) {
        if(log) {
            if(errors.isEmpty()) {
                System.out.println("msg() '"+result+"' (no errors)");
            } else {
                System.out.printf("msg() '%s' %d error(s): %s\n",
                        result,
                        errors.size(),
                        errors
                );
            }
        }
    }



}
