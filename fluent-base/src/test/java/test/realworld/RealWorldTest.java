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

package test.realworld;

import fluent.bundle.FluentResource;
import fluent.syntax.parser.*;
import org.junit.jupiter.api.Test;
import test.shared.TestLog;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// This is the 'real-world' test of the parser; it uses the Mozilla Firefox gecko_strings.ftl file.
///
/// For performance testing (at least of the parser), we use the same FTL input file but with JMH as a test harness.
public class RealWorldTest {

    private static final String RESOURCE = "perf/gecko_strings.ftl";

    @Test
    public void parseAndVerifyBundle() throws IOException {
        TestLog.println( "Input FTL: " + RESOURCE );

        // parse & ignore comments
        final FluentResource resource = FTLParser.parse(
                Thread.currentThread().getContextClassLoader(),
                RESOURCE,
                FTLParser.ParseOptions.DEFAULT
        );
        assertEquals( 493, resource.entries().size(), () -> "Resource entries: " + resource.errors() );
        assertEquals( 0, resource.errors().size(), () -> "Parse errors: " + resource.errors() );
    }
}