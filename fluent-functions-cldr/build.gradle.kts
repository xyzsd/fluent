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

plugins {
    id("fluent.java-library-conventions")
    id("com.vanniktech.maven.publish") version "0.31.0"
    `java-library`
}

dependencies {
    implementation("net.xyzsd.plurals:cldr-plural-rules:41")
    api(project(":fluent-base"))
}

tasks.jar {
    val fullModuleName = ((project.group as String) + "." + project.name)
        .replace("-", "_")

    manifest {
        attributes("Automatic-Module-Name" to fullModuleName)
    }
}

mavenPublishing {
    configure(JavaLibrary( JavadocJar.Javadoc(), true))

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

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