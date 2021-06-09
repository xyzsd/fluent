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

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// CLDR String function tests
public class ICUStringFns {

    static final CommonTest t = CommonTest.init(
            ICUFunctionFactory.INSTANCE,
            Locale.US,
            true
    );

    // no-arg
    @Test
    void noArgCASE() {
        String ftl = "msg = No argument test {CASE()}.\n";
        assertEquals(
                "No argument test {CASE()}.",
                t.msg( ftl, "msg" )
        );
    }

    // check that non-string values are passed through
    @Test
    void passthroughCASE() {
        String ftl = "msg = Passthrough {CASE($arg)}.\n";
        assertEquals(
                "Passthrough -5.",
                t.msg( ftl, "msg", Map.of("arg", -5) )
        );
    }

    // default (uppercase)
    @Test
    void defaultCASE() {
        String ftl = "msg = Uppercase: {CASE($arg)}.\n";
        assertEquals(
                "Uppercase: VARIEGATED.",
                t.msg( ftl, "msg", Map.of("arg", "VArieGatED") )
        );
    }

    // lowercase
    @Test
    void owerCASE() {
        String ftl = "msg = Lowercase: {CASE($arg,style:\"lower\")}.\n";
        assertEquals(
                "Lowercase: variegated.",
                t.msg( ftl, "msg", Map.of("arg", "VArieGatED") )
        );
    }

    // uppercase
    @Test
    void upperCASE() {
        String ftl = "msg = Uppercase: {CASE($arg,style:\"upper\")}.\n";
        assertEquals(
                "Uppercase: VARIEGATED.",
                t.msg( ftl, "msg", Map.of("arg", "VArieGatED") )
        );
    }


}
