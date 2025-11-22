package fluent.examples;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.syntax.parser.FTLParser;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;


///  This example shows that selection on Lists does not work,
///  and how to implement possible solutions if selecting over items on a list is needed.
///
/// `hello.ftl` is used for all examples.
public class ListSelectionExample {


    public static void main(String[] args) throws IOException {

        // Setup the function registry. This is the most basic way to set it up, and will include
        // only the required built-in functions. The function registry can be shared by different bundles.
        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .build();

        // parse our bundle.
        final FluentResource resource = FTLParser.parse(
                // This is a simple and efficient way to get an FTL file as a resource
                Thread.currentThread().getContextClassLoader(), "hello.ftl"
        );

        // parse errors? if so, we exit. We could proceed, but some messages may not be available.
        if (!resource.errors().isEmpty()) {
            System.err.printf( "Encountered %d errors during parsing!\n", resource.errors().size() );
            resource.errors().forEach( System.err::println );
            System.exit( 1 );
        }

        // Create the FluentBundle, which is Locale dependent.
        final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .build();

        // this is one way we could verify a message exists within a bundle
        final String MESSAGE = "shared-photos";
        if (bundle.message( MESSAGE ).isEmpty()) {
            System.err.println( "WARNING: missing message " + MESSAGE );
        }

        // what happens if 'photoCount' in the Hello.ftl file is a list?
        List<Integer> PHOTOCOUNTS = List.of( 1, 2, 3, 4, 5 );

        // Now, let's try a more complex message, which uses a selector and 3 variables.
        // This doesn't make a lot of sense for this example, because we have so many variables
        System.out.println("\nCOMPLEX message:");
        final String sharedPhotoMessage = bundle.format(
                "shared-photos",    // the message key, defined in the FTL file
                Map.of(
                        "userName", "Billy",      // userName, as above
                        "photoCount", PHOTOCOUNTS,          // photoCount
                        "userGender", "male"
                )
        );
        System.out.println( sharedPhotoMessage );   // partial message, with error


        // But even with a simpler message, it will not work:
        System.out.println("\nSIMPLE message:");
        final String simpleMessage = bundle.format(
                "simple",    // the message key, defined in the FTL file
                Map.of(
                        "photoCount", PHOTOCOUNTS          // photoCount
                )
        );
        System.out.println( simpleMessage );   // partial message, with error


        // so if we wanted a list, we need to separate the selection and the list.
        // for example:
        System.out.println("\nCOMBINING FORMATTERS:");
        List<String> USERS = List.of( "Alphonso", "Betty", "Cameron", "Drake", "Elsa" );

        assert USERS.size() == PHOTOCOUNTS.size();

        // now we have a list, with the selector applied to each item.
        final List<String> list = IntStream.range( 0, USERS.size() )
                .mapToObj( i -> new AbstractMap.SimpleEntry<>( USERS.get( i ), PHOTOCOUNTS.get( i ) ) )
                .map( entry -> bundle.format( "example-selection",
                        Map.of( "userName", entry.getKey(), "photoCount", entry.getValue() ) ) )
                .toList();

        // now, format the list
        System.out.println(
                bundle.format(
                        "formatted-list",
                        Map.of( "data", list )
                )
        );
    }

}

