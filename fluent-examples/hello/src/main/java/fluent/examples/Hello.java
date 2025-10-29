package fluent.examples;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.syntax.parser.FTLParser;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;


///  A simple Hello demonstration. The code is verbose (even for Java :) for illustration.
public class Hello {


    public static void main(String[] args) throws IOException {

        // FTLStream handles the low-level operations
        // FTLParser parses the FTLStream into the data model, as a FluentResource.
        // The FTL file we load should be localised.
        // NOTE: this can throw an IOException
        final FluentResource resource = FTLParser.parse(
                // This is a simple and efficient way to get an FTL file as a resource
                 Thread.currentThread().getContextClassLoader(), "hello.ftl"
        );

        // The FTLResource contains the data model (AST).
        // It also contains any information about errors encountered during parsing.
        if (!resource.errors().isEmpty()) {
            System.err.printf("Encountered %d errors during parsing!\n", resource.errors().size());
            resource.errors().forEach(System.err::println);
            System.exit(1);
        }

        // Setup the function registry. This is the most basic way to set it up, and will include
        // only the required built-in functions. The function registry can be shared by different bundles.
        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .build();

        // Create the FluentBundle, which is Locale dependent.
        // The FluentBundle is what we use to render messages.
        final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .build();

        // Now let's render some messages.
        // Variables in the FTL message are substituted using name-value pairs stored in a Map.
        // A Map<String, ?> provides the parameters to substitute.
        //
        // This is the simplest way to format a localized message.
        // Say hello to the user, "Billy".
        final String helloUser = bundle.format(
                "hello-user",    // the message key, defined in the FTL file
                Map.of("userName", "Billy") // our single item map
        );
        System.out.println( helloUser );

        // Now, let's try a more complex message, which uses a selector and 3 variables.
        final String sharedPhotoMessage = bundle.format(
                "shared-photos",    // the message key, defined in the FTL file
                          Map.of(
                                  "userName", "Billy",      // userName, as above
                                  "photoCount", 1,          // photoCount
                                  "userGener", "male"
                          )
        );
        System.out.println( sharedPhotoMessage );


        // Example of the same message, but with different parameters.
        System.out.println(
                bundle.format( "shared-photos",
                        Map.of( "userName", "Billy",
                                "userGender", "female",
                                "photoCount", 7
                        )
                )
        );


        // Now, what about lists (we can use any collection that implements SequencedCollection) ?
        // A variable can be a SequencedCollection, as long as we are not selecting on it.
        // (so, not allowed for $photoCount or $userGender) in the above example.
        // First example, with multiple names
        // This uses the 'default' list format.
        //
        System.out.println(
                bundle.format(
                "hello-user",
                        Map.of("userName", List.of("Billy", "Willy", "Lilly"))
            )
        );

        // What if we want an 'and' between the last two names (English locale assumed)?
        // We will use the "hello-all-users" message.
        // One name:
        System.out.println(
                bundle.format(
                        "hello-all-users",
                        Map.of("users", "Billy")
                )
        );

        // One name, as a List
        System.out.println(
                bundle.format(
                        "hello-all-users",
                        Map.of("users", List.of("Billy"))
                )
        );

        // Two names
        System.out.println(
                bundle.format(
                        "hello-all-users",
                        Map.of("users", List.of("Billy", "Willy"))
                )
        );

        // Three names
        System.out.println(
                bundle.format(
                        "hello-all-users",
                        Map.of("users", List.of("Billy", "Willy", "Lilly"))
                )
        );


    }



}
