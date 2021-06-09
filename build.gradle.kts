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

//
// Build Configuration for Gradle 7
//
// In No Way is this a quintessential representation of a Gradle build configuration ... or is it?
//
// NOTES:
//      * The root project contains no source. Sources and tests are within the subprojects.
//      * Therefore, root project tasks will not execute ("NO-SOURCE").
//      * Therefore, dependencies on subproject tasks must be explicitly specified (e.g., 'aggregatedDocs')
//      * Various coding styles are used for defining gradle tasks, etc.... Eventually a single style should be chosen
//
//
// INFO SOURCES (in no particular order)
//      * reproducible builds: https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
//      * https://madhead.me/posts/no-bullshit-maven-publish/
//      * https://github.com/apache/calcite-avatica/blob/master/build.gradle.kts
//      * https://blog.solidsoft.pl/2021/02/26/unified-gradle-projects-releasing-to-maven-central-in-2021-migration-guide/
//
// FUTURE
//      * aggregated javadocs should be published.


plugins {
    id("java-library")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("signing")
    id("com.github.spotbugs") version "4.7.1"
}

allprojects {
    group = "net.xyzsd.fluent"
    version = "0.70-SNAPSHOT"          // NOTE: publish will fail if 'staging' in name
    // use 'rootProject.name' (from settings.gradle.kts) for base name

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // to create reproducible builds ...
    tasks.withType<AbstractArchiveTask>().configureEach {
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false
        dirMode = 775
        fileMode = 664
        archiveVersion.set("${project.version}")
    }

    apply<com.github.spotbugs.snom.SpotBugsPlugin>()

    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        // note: for plugin version 4.7.1 (spotbugs 4.2.2),
        //       SpotBugs will flag record class equals() methods as 'unusual'
        setEffort("default")
        setReportLevel("high")
        ignoreFailures.set(false)
    }

    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        reports.create("xml").setEnabled(false)
        reports.create("html").setEnabled(true)
    }

}


configure(subprojects) {

    apply<MavenPublishPlugin>()
    apply<JavaLibraryPlugin>()
    apply<SigningPlugin>()

    pluginManager.withPlugin("java-library") {
        configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
            toolchain.languageVersion.set(JavaLanguageVersion.of(16))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        //options.compilerArgs.add("--enable-preview")
        //options.compilerArgs.add("-Xlint:preview")
        options.isIncremental = false
        options.encoding = "UTF-8"  // this is important, particularly for tests
    }

    tasks.withType<Javadoc>().configureEach {
        val javadocOptions = options as CoreJavadocOptions
        //javadocOptions.addBooleanOption("-enable-preview", true)
        javadocOptions.addStringOption("source", "16")
        javadocOptions.addStringOption("Xdoclint:none", "-quiet")
        javadocOptions.addBooleanOption("html5",true)
        options.encoding = "UTF-8"
    }

    tasks.withType<Jar>().configureEach {
        // using automatic modules for now
        manifest {
            val moduleName = "${rootProject.group}.${project.name}";
            attributes.set("Automatic-Module-Name", moduleName)
        }
        includeEmptyDirs = false
    }

    // tests won't run if this is not present
    tasks.test {
        useJUnitPlatform()
    }

    // if not working ('no actions') try publish to maven local for testing
    configure<PublishingExtension> {
        publications {
            val main by creating(MavenPublication::class) {
                from(components["java"])
                // TODO: need aggregated docs...

                pom {
                    artifactId = project.name
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
        }

        repositories {
            maven {
                name = "OSSRH"
                setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")

                credentials {
                    // obtained from gradle.properties in GRADLE_USER_HOME
                    credentials(PasswordCredentials::class)
                }
            }
        }


    }

    // if 'no signatory' error, make sure GRADLE_USER_HOME is set to point to the
    // the gradle.properties file containing keyID/password/etc (usually in user home dir)
    configure<SigningExtension> {
        val publishing: PublishingExtension by project
        sign(publishing.publications)
    }
}



// create aggregated javadocs (combined javadocs for all subprojects)
val aggregatedDocs by tasks.registering(Javadoc::class) {
    dependsOn(":fluent-base:build")
    dependsOn(":fluent-functions-cldr:build")
    dependsOn(":fluent-functions-icu:build")

    description = "Aggregated Javadocs"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    title = "${rootProject.name} API (complete) version ${project.version}"
    options.encoding = "UTF-8"

    val javadocOptions = options as CoreJavadocOptions
    javadocOptions.addStringOption("source", "16")
    javadocOptions.addBooleanOption("-enable-preview", true)    // not strictly needed
    javadocOptions.addStringOption("Xdoclint:none", "-quiet")   // for sanity

    val sourceSets = allprojects
        .mapNotNull { it.extensions.findByType<SourceSetContainer>() }
        .map { it.named("main") }

    classpath = files(sourceSets.map { set -> set.map { it.output + it.compileClasspath } })
    setSource(sourceSets.map { set -> set.map { it.allJava } })
    setDestinationDir(file("$buildDir/javadoc"))
}


//  archive name structure: [archiveBaseName]-[archiveAppendix]-[archiveVersion]-[archiveClassifier].[archiveExtension]
val aggregatedDocsJar by tasks.registering(Zip::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    dependsOn("aggregatedDocs")
    archiveBaseName.set(rootProject.name)
    archiveVersion.set("${project.version}")
    archiveClassifier.set("aggregated-javadoc")
    archiveExtension.set("zip")
    from(aggregatedDocs)
}


