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
    id("java-library")
    id("signing")
}

version = "0.72-SNAPSHOT"
group = "net.xyzsd.fluent"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.test {
    useJUnitPlatform()
}

java {
    // IMPORTANT!
    // if withJavadocJar() or withSources() is configured here, the
    // maven-publish plugin currently being used WILL NOT name files correctly,
    // and publishing to maven central will fail.
    // (com.vanniktech.maven.publish)
    //

    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.jar {
    setPreserveFileTimestamps(false);
    setReproducibleFileOrder(true);

    // NOTE: each subproject must override the manifest to create an automatic module name
    // with the desired module name for that subproject. In the future we will create a
    // 'real' module.
}



tasks.javadoc {
    val javadocOptions = options as CoreJavadocOptions
    //javadocOptions.addStringOption("source", "16")
    //javadocOptions.addBooleanOption("-enable-preview", true)
    javadocOptions.addStringOption("Xdoclint:none", "-quiet")   // for sanity
}


dependencies {
    compileOnly("org.jetbrains:annotations:20.1.0")
    //
    testCompileOnly("org.jetbrains:annotations:20.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}