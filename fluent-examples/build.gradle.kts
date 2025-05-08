/*
 *
 *  Copyright (C) 2025, xyzsd (Zach Del)
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

plugins {
    id("fluent.java-library-conventions")
    id("application")
}

dependencies {
    // we are not using project dependencies like so:
    // implementation(project(":fluent-base"))
    //
    // instead, we are using Central Repository dependencies as we would for a
    // standalone project.
    // TODO: ** update with proper version
    implementation("net.xyzsd.fluent:fluent-base:0.72")
    implementation("net.xyzsd.fluent:fluent-functions-icu:0.72")

}

application {
    mainModule = "fluent.examples.hello"
    mainClass = "fluent.examples.hello.Hello"
}
