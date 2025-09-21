/*
 *
 *  Copyright (c) 2025, xyzsd (Zach Del)
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
 *
 */

package test.functions;

import fluent.bundle.FluentBundle;
import fluent.bundle.FluentResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.shared.FTLTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListFnTest {


    static final String RESOURCE = "functions/list_fn.ftl";
    static FluentResource resource;
    static FluentBundle bundle;

    // helper function. Variable name 'list' used.
    private String fmt(String msgID, List<?> list) {
        return FTLTestUtils.fmt(
                bundle,
                msgID,
                Map.of("list",list)
        );
    }

    @BeforeAll
    public static void parseFile() throws IOException {
        resource = FTLTestUtils.parseFile( RESOURCE );
        bundle = FTLTestUtils.basicBundleSetup( resource, false );
    }

    @Test
    public void verifyExceptions() {
        assertEquals( 0, resource.errors().size() );
    }

    @Test
    public void listFnTest() {
        // strings
        assertEquals(
                "|s1, s2, s3|",
                fmt(  "list_default", List.of("s1","s2","s3"))
        );

        // numbers
        assertEquals(
                "|1, 2, 3|",
                fmt(  "list_default", List.of(1, 2, 3))
        );

        // heterogeneous
        assertEquals(
                "|first, 2, third, 4|",
                fmt(  "list_default", List.of("first", 2, "third", 4))
        );
    }

    @Test
    public void invalidTest() {
        assertEquals(
                "|{LIST(): Named option 'unit': unrecognized. Allowed values: [AND, OR, UNITS]}|",
                fmt(  "list_bad_1", List.of("one", "two", "three"))
        );

        assertEquals(
                "|{LIST(): Named option 'reallyhuge': unrecognized. Allowed values: [WIDE, SHORT, NARROW]}|",
                fmt(  "list_bad_2", List.of("one", "two", "three"))
        );
    }

    @Test
    public void listAndOrUnitNoWidth() {
        assertEquals(
                "|one, two, three|",
                fmt(  "list_and", List.of("one", "two", "three"))
        );

        // no 'and' here; need to specify short or wide to get a '&' or 'and'
        assertEquals(
                "|one, two|",
                fmt(  "list_and", List.of("one", "two"))
        );

        assertEquals(
                "|one, two, or three|",
                fmt(  "list_or", List.of("one", "two", "three"))
        );

        assertEquals(
                "|one or two|",
                fmt(  "list_or", List.of("one", "two"))
        );

        // ... not sure best way to use 'units' form
        assertEquals(
                "|one two three|",
                fmt(  "list_unit", List.of("one", "two", "three"))
        );
    }

    @Test
    public void listWide() {
        assertEquals(
                "|one, two, and three|",
                fmt(  "list_default_wide", List.of("one", "two", "three"))
        );

        assertEquals(
                "|one, two, and three|",
                fmt(  "list_and_wide", List.of("one", "two", "three"))
        );

        assertEquals(
                "|one, two, or three|",
                fmt(  "list_or_wide", List.of("one", "two", "three"))
        );

        assertEquals(
                "|one, two, three|",
                fmt(  "list_unit_wide", List.of("one", "two", "three"))
        );
    }

    @Test
    public void listShort() {
        assertEquals(
                "|one, two, & three|",
                fmt(  "list_default_short", List.of("one", "two", "three"))
        );

        assertEquals(
                "|one, two, & three|",
                fmt(  "list_and_short", List.of("one", "two", "three"))
        );

        assertEquals(
                "|one, two, or three|",
                fmt(  "list_or_short", List.of("one", "two", "three"))
        );

        assertEquals(
                "|one, two, three|",
                fmt(  "list_unit_short", List.of("one", "two", "three"))
        );
    }

    @Test
    public void listCombos() {
        // number format passes through strings
        assertEquals(
                "|one, two, three|",
                fmt(  "list_default_percent", List.of("one", "two", "three"))
        );

        // NUMBER still formats numbers though!
        assertEquals(
                "|50%, 75%, hundred|",
                fmt(  "list_default_percent", List.of(0.5, 0.75, "hundred"))
        );

        assertEquals(
                "|25%, 50%, 75%, or 100%|",
                fmt(  "list_or_percent", List.of(0.25, 0.50, 0.75, 1.00))
        );


    }
}
