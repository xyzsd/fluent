package fluent.examples;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentFunctionRegistry;
import fluent.bundle.FluentResource;
import fluent.bundle.LRUFunctionCache;
import fluent.bundle.resolver.Scope;
import fluent.function.FluentFunction;
import fluent.function.FluentFunctionFactory;
import fluent.function.Options;
import fluent.function.ResolvedParameters;
import fluent.function.functions.numeric.OffsetFn;
import fluent.syntax.parser.FTLParser;
import fluent.syntax.parser.FTLStream;
import fluent.types.FluentNumber;
import fluent.types.FluentString;
import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/// This example shows how we can use lambdas to define simple implicit formatters during the creation of
/// the FluentFunctionRegistry.
///
public class SimpleImplicits {

    // Some classes just for this example
    // If one of these objects was sent for formatting, it would become a FluentCustom<>.
    // For example, the Person object would become a FluentCustom<Person>.
    //
    // Since it is a custom object, Person.toString() would be used for formatting, which may or may not
    // be appropriate depending upon the application.
    //
    // Options for displaying these objects:
    //      (1) do nothing. toString() is called
    //      (2) map the desired field or fields to a set of variables that can be used in FTL files.
    //          e.g., $firstName, $lastName, etc. This is the most flexible option.
    //      (3) create a default formatter, which formats the object. However, the default formatter cannot be changed
    //          once it is created in a bundle
    //      (4) create a custom function, e.g., PEOPLE(), that would format a PEOPLE object perhaps using options
    //          to display particular fields. e.g., PEOPLE($value, field:"lastName") to display the lastName.
    //
    sealed interface People {
        String firstName();

        String lastName();

        record Person(String firstName, String lastName) implements People {}

        record Employee(String firstName, String lastName, String role) implements People {}

    }

    // explicitly NOT in the People hierarchy
    record SomeoneElse(String firstName, String lastName) {}


    public static void main(String[] args) throws IOException {

        final FluentResource resource = parseFTL( "simple_implicits.ftl" );


        // Create some default formatters. We will not create a default formatter for SomeoneElse.class
        final FluentFunctionRegistry registry = FluentFunctionRegistry.builder()
                .addDefaultFormatter( People.class, (people, _) -> people.lastName() )
                .addDefaultFormatterExact( People.Employee.class, (employee, _) -> employee.lastName() + "-" + employee.role() )
                .addFactory( PeopleFn.PEOPLE )
                .build();


        final FluentBundle bundle = FluentBundle.builder( Locale.US, registry, LRUFunctionCache.of() )
                .addResource( resource )
                .build();

        System.out.println( "\n-- Default formatting examples. Using message 'search-result'" );
        // no default formatter. will display 'SomeoneElse[firstName=John, lastName=Doe]' ...
        System.out.println(
                bundle.format( "search-result", Map.of( "value", new SomeoneElse( "John", "Doe" ) ) )
        );

        // Uses default formatter (just last name)
        System.out.println(
                bundle.format( "search-result", Map.of( "value", new People.Person( "Blair", "Blairison" ) ) )
        );

        // uses exact formatter (last name and role); exact formatter overrides the default formatter.
        System.out.println(
                bundle.format( "search-result", Map.of( "value", new People.Employee( "Blake", "Bilkerson", "CTO" ) ) )
        );


        // mapped. using our map() method defined below.
        System.out.println( "\n-- Using message 'search-result-mapped'" );
        System.out.println(
                bundle.format( "search-result-mapped", map(
                        new People.Person( "James", "Jamison" )
                ) )
        );

        System.out.println(
                bundle.format( "search-result-mapped", map(
                        new People.Employee( "Stella", "Stellarator", "Comptroller" )
                ) )
        );

        System.out.println(
                bundle.format( "search-result-mapped", map(
                        new People.Employee( "Nona", "Nonesuch", "none" )
                ) )
        );


        // fancy mapping with a selector
        System.out.println( "\n-- Using message 'search-result-mapped-fancy'" );
        System.out.println(
                bundle.format( "search-result-mapped-fancy", map(
                        new People.Person( "James", "Jamison" )
                ) )
        );

        System.out.println(
                bundle.format( "search-result-mapped-fancy", map(
                        new People.Employee( "Stella", "Stellarator", "Comptroller" )
                ) )
        );

        System.out.println(
                bundle.format( "search-result-mapped-fancy", map(
                        new People.Employee( "Nona", "Nonesuch", "none" )
                ) )
        );

        System.out.println( "\n-- Using message 'search-role', which uses the custom PEOPLE function" );
        System.out.println(
                bundle.format( "search-role", Map.of("value", new People.Employee( "Bartholomew", "Simpson", "El Presidente" ) ) )
        );

    }

    // Field mapper. A mapper could be defined within the object, too.
    static Map<String, Object> map(People people) {
        return switch (people) {
            case People.Person person ->
                    Map.of( "firstName", person.firstName(), "lastName", person.lastName(), "role", "none" );
            case People.Employee employee ->
                    Map.of( "firstName", employee.firstName(), "lastName", employee.lastName(), "role", employee.role() );
        };
    }


    // a custom function, called PEOPLE
    // enums are a nice way of creating simple functions
    @NullMarked
    enum PeopleFn implements FluentFunctionFactory<FluentFunction.Transform> {
        PEOPLE;

        @Override
        public FluentFunction.Transform create(Locale locale, Options options) {
            return new PeopleTransform( options.asEnum( PeopleField.class, "field" ).orElseThrow() );
        }

        @Override
        public boolean canCache() {
            return true;
        }

        private enum PeopleField {
            FIRSTNAME, LASTNAME, ROLE;
        }

        record PeopleTransform(PeopleField field) implements FluentFunction.Transform {

            @Override
            public List<FluentValue<?>> apply(ResolvedParameters parameters, Scope scope) {
                final var biConsumer = FluentFunction.mapOrPassthrough( People.class, this::extract );
                return parameters.positionals()
                        .mapMulti( biConsumer )
                        .toList();
            }

            private FluentString extract(People people) {
                return switch(field) {
                    case  FIRSTNAME -> FluentString.of( people.firstName() );
                    case LASTNAME -> FluentString.of( people.lastName() );
                    case ROLE -> {
                        if (people instanceof People.Employee employee) {
                            yield FluentString.of( employee.role() );
                        } else {
                            yield FluentString.of( "none" );
                        }
                    }
                };
            }


        }
    }

    // Simple method for loading FTL and checking for errors.
    private static FluentResource parseFTL(String resourceName) throws IOException {
        final FluentResource resource = FTLParser.parse(
                // This is a simple and efficient way to get an FTL file as a resource
                FTLStream.from( Thread.currentThread().getContextClassLoader(), resourceName )
        );

        if (!resource.errors().isEmpty()) {
            System.err.printf( "Encountered %d errors during parsing!\n", resource.errors().size() );
            resource.errors().forEach( System.err::println );
            System.exit( 1 );
        }

        return resource;

    }

}
