package fluent.examples;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.syntax.parser.FTLParser;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// Error Handling examples.
///
/// These examples illustrate how to log and potentially handle errors that could occur during message rendering.
///
///  This is modified from Hello.java (fluent-examples/hello)
public class ErrorHandling {

    /// We could easily implement this as a lambda, too
    static class SimpleHandler implements Consumer<FluentBundle.ErrorContext> {
        @Override
        public void accept(FluentBundle.ErrorContext errorContext) {
            // one would most likely call the logging framework here.
            System.err.printf("Error occurred parsing: message %s for bundle locale %s\n", errorContext.entryName(), errorContext.locale());
            errorContext.exceptions().forEach( System.err::println );
        }
    }

    public static void main(String[] args) throws IOException {

        // Parse the FTL into a FluentResource.
        final FluentResource resource = FTLParser.parse(
                // This is a simple and efficient way to get an FTL file as a resource
                 Thread.currentThread().getContextClassLoader(), "hello.ftl"
        );

        // The FluentResource contains the data model (AST).
        // It also contains any information about errors encountered during parsing.
        if (!resource.errors().isEmpty()) {
            // Note that messages that do not parse successfully will not be added to the resource, but messages
            // that DO parse correctly will be. So if a message is missing, it could be because it did not parse
            // correclty.
            System.err.printf("Encountered %d errors during parsing!\n", resource.errors().size());
            resource.errors().forEach(System.err::println);
            System.exit(1);
        }


        // Setup a basic function registry.
        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .build();

        // Create the FluentBundle, which is Locale dependent.
        // The FluentBundle is what we use to render messages.
        final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .withLogger( new SimpleHandler() )
                .build();

        // Now let's render some messages.
        final String helloUser = bundle.format(
                "hello-user",    // the message key, defined in the FTL file
                Map.of("userName", "Billy") // our single item map
        );
        System.out.println( helloUser );

        // But this message does not exist!
        // see what happens:
        final String helloRobot = bundle.format(
                "hello-robot",              // the message key, defined in the FTL file, which is missing! oh no!
                Map.of("userName", "Bender")    // our single item map
        );
        System.out.println( helloRobot );         //{Unknown message: 'hello-robot'}, but also displays an error on System.err


        // an alternative approach:
        final String userName = "Billy";
        String out = bundle.fmtBuilder( "hello-user" )
                .arg( "userName", userName )    // or we could use: .args( Map.of("userName", userName) )
                .formatOrElse( "Hello "+userName );
        System.out.println( out );      // prints our message: "Hello, Billy!"

        final String robotName = "Bender";
        out = bundle.fmtBuilder( "hello-robot" )
                .arg( "robotName", robotName )
                .formatOrElse( "HELLO ROBOT "+robotName );
        System.out.println( out );      // prints our fallback message "HELLO ROBOT Bender"

        // another example.
        out = bundle.fmtBuilder( "hello-user" )
                // 'robotName' specified, but does not exist in message (this is NOT an error)
                // 'userName' NOT specified, which is needed for the message
                .arg( "robotName", userName )
                .formatOrElseGet( () -> "HELLO "+userName+"!" );    // using a Supplier, just for illustration
        System.out.println( out );      // prints our fallback message: ""HELLO Billy!"

        // we could also throw an exception
        try {
            out = bundle.fmtBuilder( "hello-user" )
                    // 'robotName' specified, but does not exist in message (this is NOT an error)
                    // 'userName' NOT specified, which is needed for the message
                    .arg( "robotName", userName )
                    .formatOrThrow( () -> new IllegalStateException( "missing 'hello-user' message!" ) );
            System.out.println( out );  // doesn't execute
        } catch ( IllegalStateException e ) {
            System.out.println("!!Caught exception: "+e);
        }

        // here we have a String supplier that throws an exception!
        Supplier<String> badSupplier = () -> { throw new IllegalStateException("string supplier failed!"); };

        // this will result in two errors in the ErrorContext:
        //  (1) ReferenceException (missing '$userName')
        //  (2) FallbackFailure (String supplier failed to supply a String)
        out = bundle.fmtBuilder( "hello-user" )
                // 'robotName' specified, but does not exist in message (this is NOT an error)
                // 'userName' NOT specified, which is needed for the message
                .arg( "robotName", userName )
                .formatOrElseGet( badSupplier );
        System.out.println( out );      // {Message 'hello-user' fallback failure!}


        // A more complex example. Use a backup key ... if possible.
        Supplier<String> formatter = () ->  bundle.format( "hello-user" , Map.of("userName", userName));
        out = bundle.fmtBuilder( "hello-all-users" )
                .arg( "users", userName )
                .formatOrElseGet( formatter );
        System.out.println( out );      // "Hello, Billy!"

        out = bundle.fmtBuilder( "hello-robot" )
                .arg( "robotName", robotName )
                .formatOrElseGet( formatter );
        System.out.println( out );      // "Hello, Billy!"


    }



}
