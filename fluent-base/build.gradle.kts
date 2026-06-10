/*
 *
 *  Copyright (C) 2025, 2026 xyzsd (Zach Del)
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
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort

plugins {
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("com.github.spotbugs") version "6.5.5"
    id("signing")
    id("java-library")
    id("me.champeau.jmh") version "0.7.3"
}

version = "2.0"
group = "net.xyzsd.fluent"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    api("org.jspecify:jspecify:1.0.0")
    implementation("com.ibm.icu:icu4j:78.1")
    //
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


jmh {
    warmupIterations = 3
    iterations = 10
    fork = 1
    jmhVersion = "1.37"

}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all,-serial")
}

java {
    // IMPORTANT!
    // if withJavadocJar() or withSources() is configured here, the
    // maven-publish plugin currently being used WILL NOT name files correctly,
    // and publishing to maven central will fail (for subprojects)
    // (com.vanniktech.maven.publish)
    // NOTE: the above may need to be re-verified with more recent versions

    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks.javadoc {
    val javadocOptions = options as CoreJavadocOptions
    javadocOptions.addStringOption("Xdoclint:none", "-quiet")   // for sanity
}

spotbugs {
    //  don't break the build on failures.
    ignoreFailures = true
    effort = Effort.MAX
    reportLevel = Confidence.LOW
    // our format strings are specific, so we will disable "FormatStringChecker".
    omitVisitors = listOf("FormatStringChecker")
    excludeFilter = file("spotbugs_exclude.xml")
}

mavenPublishing {
    project.logger.lifecycle("Publishing: Coordinates: "+project.group+":"+project.name+":"+project.version)

    // for now, we will disable automatic release.
    publishToMavenCentral(automaticRelease = false)
    signAllPublications()

    configure( JavaLibrary(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = SourcesJar.Sources()
    ))

    coordinates(groupId = project.group as String, project.name, project.version as String)

    pom {

        name.set("Project Fluent for Java")
        description.set("A Java implementation of Mozilla Project Fluent")
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


signing {
    val githubCI: Boolean = "true".equals(System.getenv("CI"))
    if (githubCI) {
        project.logger.lifecycle("Signing: Using Github CI environment.")
        val signingKey: String? = System.getenv("SIGNING_KEY_PRIVATE")
        val signingKeyPassphrase: String? = System.getenv("SIGNING_KEY_PASSPHRASE")
        useInMemoryPgpKeys(signingKey, signingKeyPassphrase)
    } else {
        project.logger.lifecycle("Signing: Using local credentials.")
        useGpgCmd()
    }
}


