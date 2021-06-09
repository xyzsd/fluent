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

import fluent.functions.icu.ICUFunctionFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FTL Select tests
 */
public class ICUSelectTests {

    static final CommonTest t = CommonTest.init(
            ICUFunctionFactory.INSTANCE,
            Locale.US,
            true
    );


    @Test
    void selectorTermTest() {
        String ftl = """
                -thing = { $count ->
                    [one] thing
                   *[other] things
                 }
                
                you-own = You have { $count ->
                     [one] a {-thing(count: "one")}
                    *[other] {-thing(count: "other")}
                 }.
                 
                """;

        assertEquals(
                "You have a thing.",
                t.msg( ftl, "you-own" , Map.of("count", "one") )
        );

        assertEquals(
                "You have things.",
                t.msg( ftl, "you-own" , Map.of("count", "ten") )
        );
    }


    @Test
    void stringSelectTest() {
        String ftl = """
            joke = Why did the Java programmer { $place -> 
                    [orchard] work in the orchard? To try pear programming.
                    [winter] only work in the winter? Because he did not like Spring.
                   *[other] ... hey, make up your own joke. The programmer was in '{$place}'.
             }
            """;

        // missing variable : take default path
        assertEquals(
                "Why did the Java programmer ... hey, make up your own joke. The programmer was in '{$place}'.",
                t.msg( ftl, "joke", Map.of() )
        );

        assertEquals(
                "Why did the Java programmer work in the orchard? To try pear programming.",
                t.msg( ftl, "joke", Map.of("place","orchard") )
        );

        assertEquals(
                "Why did the Java programmer only work in the winter? Because he did not like Spring.",
                t.msg( ftl, "joke", Map.of("place","winter") )
        );

        assertEquals(
                "Why did the Java programmer ... hey, make up your own joke. The programmer was in 'hiding'.",
                t.msg( ftl, "joke", Map.of("place","hiding") )
        );
    }



    @Test
    void numberSelectImplicitTest() {
        // when selecting on a number type, the default (implicit) choice is to convert to a cardinal plural category
        // for Locale.EN:
        //      https://unicode-org.github.io/cldr-staging/charts/37/supplemental/language_plural_rules.html
        //      only '1' (i=1, v=0) results in 'one' category.
        //      all others result in 'other'
        //      e.g.: I have 1 apple. I have 1.0 apples. I have 1.3 apples. I have 2 apples. etc.

        String ftl = """
                msg = The message is { $num ->
                        [0] zero. Value: '{$num}'.
                        [100] one-hundred. Value: '{$num}'.
                        [1000] one-thousand. Value: '{$num}'.
                        [zero] zero-by-plural select. Value: '{$num}'.
                        [one] one-by-plural-select. Value: '{$num}'.
                        [two] TWO-by-plural-select. Value: '{$num}'.
                        [few] A FEW-by-plural-select. Value: '{$num}'.
                        [other] other-by-plural-select. Value: '{$num}'.         
                       *[yikes] THIS ONLY OCCURS IF THERE IS AN ERROR. Value: '{$num}'.
                 }
                """;


        assertEquals(
                "The message is other-by-plural-select. Value: '0'.",
                t.msg( ftl, "msg", Map.of( "num", 0 ) )
        );


        assertEquals(
                "The message is one-by-plural-select. Value: '1'.",
                t.msg( ftl, "msg", Map.of( "num", 1 ) )
        );

        assertEquals(
                "The message is other-by-plural-select. Value: '1'.",
                t.msg( ftl, "msg", Map.of( "num", new BigDecimal( "1.0" ) ) )
        );

        // the plural category is correct, for '1.0' but
        // Decimalformat will format '1.0' as '1' unless options are used during formatting.
        assertEquals(
                "The message is other-by-plural-select. Value: '1'.",
                t.msg( ftl, "msg", Map.of( "num", 1.0d ) )
        );



        // assumes Locale.EN with standard number formatting
        assertEquals(
                "The message is other-by-plural-select. Value: '1,000'.",
                t.msg( ftl, "msg", Map.of( "num", 1000 ) )
        );
    }


    @Test
    void numberSelectExplicitString() {
        // number as a selector, but use the formatted result as a string.
        // this form (type:"string") should be used with caution!
        // NOTE: number formatting occurs before string conversion.
        // grouping separators SHOULD NOT be used (cannot have commas in selectors)
        // but negative numbers and numbers with decimals are allowed as selectors
        //
        // NOTE: if the grouping separator is used, and is present in the formatted result,
        //       the default variant will be matched!
        String ftl = """
                msg = The message is { NUMBER($num, useGrouping:"false", type:"string") -> 
                        [0] zero. String select. Value: '{$num}'.
                        [100] one-hundred. String select. Value: '{$num}'.
                        [1000] one-thousand. String select. Value: '{$num}'.
                        [zero] zero-by-plural select. Value: '{$num}'.
                        [one] one-by-plural-select. Value: '{$num}'.
                        [two] TWO-by-plural-select. Value: '{$num}'.
                        [few] A FEW-by-plural-select. Value: '{$num}'.
                        [other] other-by-plural-select. Value: '{$num}'.          
                       *[yikes] THIS IS THE DEFAULT. Value: '{$num}'.
                 }
                """;

        assertEquals(
                "The message is one-hundred. String select. Value: '100'.",
                t.msg( ftl, "msg", Map.of( "num", 100 ) )
        );

        assertEquals(
                "The message is zero. String select. Value: '0'.",
                t.msg( ftl, "msg", Map.of( "num", 0 ) )
        );

        assertEquals(
                "The message is one-thousand. String select. Value: '1,000'.",
                t.msg( ftl, "msg", Map.of( "num", 1000 ) )
        );

        // nonmatched string; use default
        assertEquals(
                "The message is THIS IS THE DEFAULT. Value: '9,999'.",
                t.msg( ftl, "msg", Map.of( "num", 9999 ) )
        );
    }

    @Test
    void numberSelectExplicitOrdinal() {
        // ordinal test
        // if we explicitly use the NUMBER() function in select, we MUST specify the type
        // if type is not specified, NUMBER() will convert the value to a String and the
        // selector will be matched with the string (same as type:"string")
        // for English:
        //      one, two, few, other
        String ftl = """
            msg = The message is { NUMBER($num, type:"ordinal") -> 
                    [0] zero. Value: '{$num}'.
                    [100] one-hundred. Value: '{$num}'.
                    [1000] one-thousand. Value: '{$num}'.
                    [zero] zero-by-plural select. Value: '{$num}'.
                    [one] ONE-by-plural-select. Value: '{$num}'.
                    [two] TWO-by-plural-select. Value: '{$num}'.
                    [few] A FEW-by-plural-select. Value: '{$num}'.
                    [other] OTHER-by-plural-select. Value: '{$num}'.          
                   *[yikes] ERROR OCCURED: Value: '{$num}' selected via '{NUMBER($num, type:"ordinal")}'.
             }
            """;

        assertEquals(
                "The message is ONE-by-plural-select. Value: '1'.",
                t.msg( ftl, "msg", Map.of("num",1) )
        );

        assertEquals(
                "The message is TWO-by-plural-select. Value: '22'.",
                t.msg( ftl, "msg", Map.of("num",22) )
        );

        assertEquals(
                "The message is A FEW-by-plural-select. Value: '33'.",
                t.msg( ftl, "msg", Map.of("num",33) )
        );

        assertEquals(
                "The message is OTHER-by-plural-select. Value: '590'.",
                t.msg( ftl, "msg", Map.of("num",590) )
        );

    }


    @Test
    void selectStringOverList() {
        // for lists, the select pattern should apply to EACH item. The results of the select are then
        // JOINed implicitly.
        // What we cannot -- yet -- do is get a value for the single item being selected.
        //
        String ftl = """
            msg = The message is { $var -> 
                    [alpha] [ALPHA]
                    [beta] [BETA]
                    [gamma] [GAMMA]
                    *[unknown] [UNKNOWN]
             }
            """;

        assertEquals(
                "The message is [BETA], [ALPHA], [GAMMA], [UNKNOWN]",
                t.msg( ftl, "msg", Map.of("var",
                        List.of("beta","alpha","gamma","delta")) )
        );
    }


}
