package fluent.examples.hello;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import fluent.functions.icu.ICUFunctionFactory;
import fluent.syntax.parser.FTLParser;
import fluent.syntax.parser.FTLStream;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class Hello {


    public static void main(String[] args) throws Exception {

        // FTLStream handles the low-level operations
        // FTLParser parses the stream into the data model
        final FluentResource parsedFTL = FTLParser.parse(
                FTLStream.of(
                        getResourceAsChars( Hello.class, "hello.ftl" )
                )
        );


        // Create the FluentBundle, which allows us to manipulate the data model
        FluentBundle bundle = FluentBundle.builder( Locale.US, ICUFunctionFactory.INSTANCE )
                .addResource( parsedFTL )
                .build();

        // Use the bundle. These are the simplest methods to format a localized message.
        // A Map<String, ?> provides the parameters to substitute.
        final String helloUser = bundle.format(
                "hello-user",                       // the message name (key)
                Map.of( "userName", "Billy" )      // Map of variables to substitute
        );
        System.out.println( helloUser );


        // Example of the same message, with different parameters.
        System.out.println(
                bundle.format( "shared-photos",
                        Map.of( "userName", "Anne",
                                "userGender", "female",
                                "photoCount", 1
                        )
                )
        );

        System.out.println(
                bundle.format( "shared-photos",
                        Map.of( "userName", "Billy",
                                "userGender", "male",
                                "photoCount", 5
                        )
                )
        );

        System.out.println(
                bundle.format( "shared-photos",
                        Map.of( "userName", "Chris",
                                "userGender", "unspecified",
                                "photoCount", 10
                        )
                )
        );


        // simple example using lists
        Map<String, ?> argsList2 = Map.of("userName", List.of("Betty", "Yeti"));
        Map<String, ?> argsList3 = Map.of("userName", List.of("Billy", "Silly", "Willy"));

        System.out.println( bundle.format("hello-user", argsList3) );

        // and now with FTL customized for lists
        FluentBundle listBundle = FluentBundle.builder( Locale.US, ICUFunctionFactory.INSTANCE )
                .addResource( FTLParser.parse(FTLStream.of( getResourceAsChars( Hello.class, "list.ftl" ) )) )
                .build();

        System.out.println( listBundle.format( "hello-users", argsList2 ) );
        System.out.println( listBundle.format( "hello-users", argsList3 ) );

    }




    // for this example, the "hello.ftl" may be referred to as:
    //      "hello.ftl"
    //      "/fluent/example/hello/hello.ftl"
    static char[] getResourceAsChars(Class<?> cls, final String resourceName) throws IOException {
        try (InputStream is = cls.getResourceAsStream( resourceName )) {
            if (is == null) {
                throw new IOException( "Missing resource: " + resourceName );
            }

            try (InputStreamReader isr = new InputStreamReader( is, StandardCharsets.UTF_8 );
                 CharArrayWriter writer = new CharArrayWriter( 8192 )) {
                isr.transferTo( writer );
                return writer.toCharArray();
            }
        }
    }

}
