plugins {
    id("application")
}

group = "fluent.examples"
version = "unspecified"

repositories {
    mavenCentral()
}


application {
    mainClass = "fluent.examples.Hello"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks.compileJava {
    options.compilerArgs.add("--enable-preview")
}

dependencies {
    // use local dependency first
    implementation(project(":fluent-base"))
    // TODO:    add this line and test. if above is not present, use maven dependency
    //          implementation("net.xyzsd.fluent:fluent-base:**VERSION**")
}

tasks.test {
    useJUnitPlatform()
}