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

import fluent.functions.cldr.CLDRFunctionFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CLDRMiscTests {

    static final CommonTest t = CommonTest.init(
            CLDRFunctionFactory.INSTANCE,
            Locale.US,
            true
    );

    
    @Test
    void functionErrorTest() {
        String ftl = """
                fn0 = { THISFUNCTIONDOESNOTEXIST() }
                fn1 = { THISFUNCTIONDOESNOTEXIST($andhasamissingvariable) }
                fn2 = { SIGN(ABS("notanumber")) }
                fn3 = { SIGN(ABS($missingvariable)) }
                fn4 = { COUNT(SIGN(ABS($missingvariable))) }
                fn5 = { COUNT(SIGN(ABS(5), $missingvariable)) }
                """;

        assertEquals( "{THISFUNCTIONDOESNOTEXIST()}",
                t.msg( ftl, "fn0" ) );
        assertEquals( "{THISFUNCTIONDOESNOTEXIST()}",
                t.msg( ftl, "fn1" ) );

        // ABS() and SIGN() are permissive; non-numeric values are passed through:
        assertEquals( "notanumber", t.msg( ftl, "fn2" ) );
        // but errors are not, and we want to fail on the function that actually
        // causes the error, even when nested
        assertEquals( "{ABS()}", t.msg( ftl, "fn3" ) );
        assertEquals( "{ABS()}", t.msg( ftl, "fn4" ) );
        assertEquals( "{SIGN()}", t.msg( ftl, "fn5" ) );

    }



    @Test
    void functionlistTest() {
        String ftl = """
                fn10 = { COUNT("arg") }
                fn20 = { COUNT("arg1","arg2") }
                fn30 = { COUNT($mylist) }
                fn40 = { COUNT($mylist, $nolist) }
                fn45 = { COUNT($mylist, $mylist) }
                fn50 = { JOIN($mylist, $nolist) }
                """;

        final Map<String, ?> map = Map.of(
                "mylist", List.of("item_1","item_2","item_3"),
                "nolist", "single_item"
        );

        assertEquals(
                "1",
                t.msg( ftl, "fn10" , map )
        );

        assertEquals(
                "2",
                t.msg( ftl, "fn20", map  )
        );

        assertEquals(
                "3",
                t.msg( ftl, "fn30" , map )
        );

        assertEquals(
                "4",
                t.msg( ftl, "fn40" , map )
        );

        assertEquals(
                "6",
                t.msg( ftl, "fn45" , map )
        );

        assertEquals(
                "item_1, item_2, item_3, single_item",
                t.msg( ftl, "fn50" , map )
        );
    }


    @Test
    void functionSetTest() {
        String ftl = """
                fn1 = { COUNT($myset) }
                """;

        final Map<String, ?> map = Map.of(
                "myset", Set.of( "item_1", "item_2", "item_3" )
                );

        assertEquals(
                "3",
                t.msg( ftl, "fn1", map )
        );

    }
}
