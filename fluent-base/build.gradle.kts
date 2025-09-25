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
import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort

plugins {
    id("com.vanniktech.maven.publish") version "0.31.0"
    // https://github.com/spotbugs/spotbugs-gradle-plugin
    id("com.github.spotbugs") version "6.2.2"
    id("signing")
    id("java-library")
    // https://github.com/melix/jmh-gradle-plugin
    id("me.champeau.jmh") version "0.7.3"
}

version = "1.9NG-SNAPSHOT"
group = "net.xyzsd.fluent"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-modules", "jdk.incubator.vector")
}

dependencies {
    api("org.jspecify:jspecify:1.0.0")
    implementation("com.ibm.icu:icu4j:77.1")
    //
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


java {
    // IMPORTANT!
    // if withJavadocJar() or withSources() is configured here, the
    // maven-publish plugin currently being used WILL NOT name files correctly,
    // and publishing to maven central will fail (for subprojects)
    // (com.vanniktech.maven.publish)
    //

    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }


}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview"))
    options.compilerArgs.add("--add-modules")
    options.compilerArgs.add("jdk.incubator.vector")
}



jmh {
    warmupIterations = 1
    iterations = 10
    fork = 2
    jmhVersion = "1.37"
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all,-dangling-doc-comments,-serial")
}

tasks.jar {
    setPreserveFileTimestamps(false)
    setReproducibleFileOrder(true)
}

tasks.javadoc {
    val javadocOptions = options as CoreJavadocOptions
    //javadocOptions.addStringOption("source", "21")
    //javadocOptions.addBooleanOption("-enable-preview", true)
    javadocOptions.addStringOption("Xdoclint:none", "-quiet")   // for sanity
}

spotbugs {
    // for now, we won't break the build on failures.
    ignoreFailures = true
    effort = Effort.MAX
    reportLevel = Confidence.LOW
    // our format strings are specific, so we will disable "FormatStringChecker".
    omitVisitors = listOf("FormatStringChecker")
    excludeFilter = file("spotbugs_exclude.xml")
}

mavenPublishing {
    configure(JavaLibrary( JavadocJar.Javadoc(), true))

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    //coordinates(groupId = project.group as String, "fluent-base", "0.72-SNAPSHOT")

    pom {
        name.set("Project Fluent for Java")
        description.set("A Java implementation of the Mozilla Project Fluent ")
        url.set("https://github.com/xyzsd/fluent")
        inceptionYear.set("2021")


        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                comments.set("A business-friendly OSS license")
            }
            license {
                name.set("The MIT License")
                url.set("https://opensource.org/licenses/MIT")
                comments.set("A GPL/LGPL compatible OSS license")
            }
        }

        developers {
            developer {
                id.set("xyzsd")
                name.set("Zach Del")
                email.set("xyzsd@xyzsd.net")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/xyzsd/fluent.git")
            developerConnection.set("scm:git:ssh://git@github.com:xyzsd/fluent.git")
            url.set("https://github.com/xyzsd/fluent")
        }
    }
}


