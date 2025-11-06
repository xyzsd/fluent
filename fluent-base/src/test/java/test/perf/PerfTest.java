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

package test.perf;

import fluent.bundle.FluentResource;
import fluent.syntax.parser.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerfTest {

    private static final String RESOURCE = "perf/gecko_strings.ftl";
    private static final int ITERATIONS = 10000;

    // really we are just validating the PerfTest file here
    @Test
    public void parseAndVerifyBundle() throws IOException {
        System.out.println( "Input FTL: " + RESOURCE );

        int count = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            // parse & ignore comments
            final FluentResource resource = FTLParser.parse(
                    Thread.currentThread().getContextClassLoader(),
                    RESOURCE,
                    FTLParser.ParseOptions.DEFAULT,
                    FTLParser.Implementation.SCALAR
            );
            count += resource.entries().size();
            assertEquals( 493, resource.entries().size() );
            assertEquals( 0, resource.errors().size() );
        }
        System.out.println( count );
    }
}
