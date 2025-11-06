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

package parser;

import fluent.bundle.FluentResource;
import fluent.syntax.parser.FTLParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class ParserBench {

    /*
11/5/2025
=========
Using the current implementation:
Benchmark                        Mode  Cnt    Score   Error  Units
ParserBench.measureParserSIMD    avgt    6  130.459   3.338  us/op
ParserBench.measureParserScalar  avgt    6  128.585   2.749  us/op

Another run:
Benchmark                        Mode  Cnt    Score   Error  Units
ParserBench.measureParserSIMD    avgt   10  151.107 � 2.541  us/op
ParserBench.measureParserScalar  avgt   10  130.561 � 2.909  us/op

Using methodhandles/invokestatic, performance was worse though not by that much

Single shot time : SIMD parser is about 4-5x slower

Testing just using the following methods SIMD vs. SCALAR, all other scalar:
SIMD: getIdentifier
    SIMD is faster by about 5%
SBB/SBBNLC vs scalar
    SIMD NOT faster; slightly slower
SBInline
    SIMD ~10% slower
SkipBlank
    about the same (within margin of error)
skipToEOL
    about the same (within margin of error)

     */

    private static final String RESOURCE = "gecko_strings.ftl";


    @Benchmark
    public void measureParserSIMD(Blackhole blackhole) throws IOException {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final FluentResource parsedResource = FTLParser.parse(
                contextClassLoader, RESOURCE, FTLParser.ParseOptions.DEFAULT, FTLParser.Implementation.SIMD
        );
        blackhole.consume( parsedResource );
    }

    @Benchmark
    public void measureParserScalar(Blackhole blackhole) throws IOException {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final FluentResource parsedResource = FTLParser.parse(
                 contextClassLoader, RESOURCE, FTLParser.ParseOptions.DEFAULT, FTLParser.Implementation.SCALAR
        );
        blackhole.consume( parsedResource );
    }







}
