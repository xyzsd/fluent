package fluent.examples;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.syntax.parser.FTLParser;

import java.io.*;
import java.util.List;
import java.util.Locale;


/// This examples shows how we could select a Locale, given the users current locale (or selected locale) from
/// the locales for which we have translations.
///
public class LocaleSelectionExample {

    // NOTE
    // There are many ways to perform selection and organization.
    // Let's say we have two localizations, for English and French.
    // we could name the ftl as so:
    //      hello_en.ftl
    //      hello_fr.ftl
    //      hello.ftl           // this could be our 'root' locale
    // or within directories
    //      root/hello.ftl      // 'und' would also be CLDR-appropriate so we could name this und/hello.ftl
    //      en/hello.ftl
    //      fr/hello.ftl
    // or via some other mechanism (query a server, etc.)
    //
    // In any of the above cases, though, we need to know WHAT locales are available.
    // (e.g., "english" and "french"), and what locale is the root locale (e.g., if no locale matches at all, what
    // locale should we use.
    //
    // How we enumerate the available locales could be via a request (e.g., to a database or server),
    // hardcoded, via a properties file or other simple resource on the classpath, or via enumerating classpath
    // resources (if you dare), etc.
    //
    // For this example, which we want to keep locale, we will use a simple resource entry describing available locales.
    // Locales will be organized by directory.


    public static void main(String[] args) throws IOException {

        // In this example, we have resource directories labelled by the BCP 47 language tag.
        // A simply structured files tells us which language tags are available.
        //
        // A VERY IMPORTANT NOTE
        // It is best to have a base language, and then subvariants.
        // e.g., for both US english and British english, tags should be
        // "en" and "en-GB"   OR  "en" and "en-US".
        // Otherwise fallback will not work well at all.
        // for example, if we have "fr-FR" but the user is French-Canadian ("fr-FR") (and "fr-FR" is not available),
        // the matching algorithm will return "fr", NOT "fr-FR" as the closest match (yes...)
        // And if "fr" doesn't exist, then we could default to the root locale.

        List<Locale> availableLocales = findAvailableLocales( "available-locales" );

        // we could match via user preferences (see Locale.LanguageRange), if we are aware
        // e.g.:   String pref = "en-US;q=1.0,en-GB;q=0.5"
        //
        // String userLanguagePref =  "en-US;q=1.0,en-GB;q=0.5";
        // List<Locale.LanguageRange> languageRanges = Locale.LanguageRange.parse(userLanguagePref);
        //
        // Then this would be the preferred list, based on what is available.
        // final List<Locale> locales = Locale.filter( languageRanges, availableLocales );
        //
        // alternatively, we could just present the choices as-seen in 'availableLocales' above.
        // or, match based on system locale/detected locale.
        // see: https://docs.oracle.com/javase/tutorial/i18n/locale/matching.html

        // e.g.: Locale.UK, Locale.US, Locale.CANADA_FRENCH, Locale.CHINA
        Locale systemLocale = Locale.CANADA_FRENCH;    // defined here for clarity, but typically obtained via Locale.getDefault()

        // we do not have fr-CA, just fr-FR.
        // so how can we match the closest?
        List<Locale.LanguageRange> systemPref = List.of( new Locale.LanguageRange( systemLocale.toLanguageTag(), 1.0 ) );

        Locale closestLocale = Locale.lookup(systemPref, availableLocales);
        if( closestLocale == null ) {
            System.out.println("Closest locale: NO MATCH (using default locale)");
            closestLocale = availableLocales.getFirst();
        }

        // NOTE: toLanguageTag() uses a hyphen as a separator, which is proper, unlike toString()
        System.out.printf("Closest locale: %s (%s)\n", closestLocale.getDisplayLanguage(), closestLocale.toLanguageTag());

        // a check
        assert availableLocales.contains(closestLocale);

        // Setup the function registry. This is the most basic way to set it up, and will include
        // only the required built-in functions. The function registry can be shared by different bundles.
        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .build();

        // Now that we have a matching locale, load the resource and create the bundle.
        // FTLParser parses FTL into the data model, as a FluentResource.
        // The FTL file we load should be localized.
        // NOTE: this can throw an IOException
        final FluentResource resource = FTLParser.parse(
                Thread.currentThread().getContextClassLoader(), closestLocale.toLanguageTag()+"/messages.ftl"
        );

        // The FluentResource contains the data model (AST).
        // It also contains any information about errors encountered during parsing.
        if (!resource.errors().isEmpty()) {
            System.err.printf( "Encountered %d errors during parsing!\n", resource.errors().size() );
            resource.errors().forEach( System.err::println );
            System.exit( 1 );
        }

        // Create the FluentBundle, which is Locale dependent.
        // The FluentBundle is what we use to render messages.
        final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .build();

        // and finally, our message (localized!)
        System.out.println( bundle.format( "example-message" ));

    }

    // Find the available locales for which we have translations (.ftl)
    // this is NOT related to Locale.getAvailableLocales() or Locale.availableLocales()
    static List<Locale> findAvailableLocales(String resourceName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( resourceName )) {
            if (is == null) {
                throw new FileNotFoundException( resourceName );
            }

            try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
                return br.lines()
                        .filter( line -> !line.isEmpty() )
                        .filter( line -> !line.startsWith( "#" ) )
                        .map( line -> Locale.forLanguageTag( line.trim() ) )
                        .toList();

            }
        }


    }

}
