package fluent.examples;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import fluent.function.functions.temporal.DateTimeFn;
import fluent.function.functions.temporal.TemporalFn;
import fluent.syntax.parser.FTLParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;


///  Here, we play with implicit functions and show how to rename functions.
public class RenamedFunctions {

    private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.of(
            LocalDateTime.of( 2025, 1, 2, 3, 4 ),
            ZoneId.of( "America/New_York" )
    );


    public static void main(String[] args) throws IOException {

        final FluentResource resource = parseFTL( "renamed.ftl" );

        System.out.println( "ZONED_DATE_TIME is defined as: '" + ZONED_DATE_TIME + "'" );

        // let's put it in a map, and call it 'date'.
        final Map<String, Object> map = Map.of( "date", ZONED_DATE_TIME );

        // We are going to setup the function registry. But for the first part, we are going to setup the registry
        // such that we use TEMPORAL instead of DATETIME.
        //
        // IN GENERAL, it is not advised to replace the default functions with functions of another name.
        // Note that the options for DATETIME and TEMPORAL are quite different. TEMPORAL requires an 'as' or 'pattern'
        // option, and will output an error message if it is missing.
        //
        // this error will also occur when formatting temporal values WITHOUT the function (default/implicit).
        // for example: "The date is now $date" results in a call to TEMPORAL. But that call will return an error,
        // as no option was set!
        //
        // However, we can specify 'default' options for a function (any function) when we setup a resource bundle.
        // (not the registry). Default options are overridden if specified in FTL.
        {
            System.out.println( "\nReplacement of required functions:" );
            final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                    .setTemporalFormatter( TemporalFn.TEMPORAL )
                    .build();


            final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                    .addResource( resource )
                    // here, we specify a default option for TEMPORAL. If this line is commented out,
                    // bundle.format("date-implicit"...) will return {ImplicitFormat of FluentTemporal<ZonedDateTime>: Missing required option 'pattern' or 'as'.}
                    .withFunctionOptions( "TEMPORAL", Options.of( "as", "ISO_LOCAL_DATE_TIME" ) )
                    .build();

            System.out.println(
                    bundle.format( "date-implicit", map )
            );

            // we replaced this with TEMPORAL, so the DATETIME function returns '{Unknown function: DATETIME()}'
            System.out.println(
                    bundle.format( "date-datetime", map )
            );

            System.out.println(
                    bundle.format( "date-temporal", map )
            );

        }

        // Now we will setup the function registry but add a function called SIMPLEDATE(), which is really the
        // DATETIME function.
        //
        // note that the name must abide by FTL rules, which is:  "[A-Z]([A-Z][0-9]_-)*"
        //
        // We also set a default option for SIMPLEDATE so that it uses "dateStyle:short" by default.
        {
            System.out.println( "\nSIMPLEDATE examples:" );
            final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                    .addFactory( FluentFunctionFactory.rename( DateTimeFn.DATETIME, "SIMPLEDATE" ) )
                    .build();

            final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                    .addResource( resource )
                    .build();

            System.out.println(
                    bundle.format( "date-implicit", map )
            );

            // this works, because DATETIME has not been replaced.
            System.out.println(
                    bundle.format( "date-datetime", map )
            );

            // {Unknown function: TEMPORAL()} since we did not add TEMPORAL to the registry.
            System.out.println(
                    bundle.format( "date-temporal", map )
            );

            System.out.println(
                    bundle.format( "date-renamed", map )
            );

        }

    }


    // Simple method for loading FTL and checking for errors.
    private static FluentResource parseFTL(String resourceName) throws IOException {
        final FluentResource resource = FTLParser.parse(
                // This is a simple and efficient way to get an FTL file as a resource
                Thread.currentThread().getContextClassLoader(), resourceName
        );

        if (!resource.errors().isEmpty()) {
            System.err.printf( "Encountered %d errors during parsing!\n", resource.errors().size() );
            resource.errors().forEach( System.err::println );
            System.exit( 1 );
        }

        return resource;

    }

}
