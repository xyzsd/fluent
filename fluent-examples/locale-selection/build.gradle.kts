plugins {
    id("application")
}

group = "fluent.examples"
version = "unspecified"

repositories {
    mavenCentral()
}


application {
    mainClass = "fluent.examples.LocaleSelectionExample"
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

    // we need ICU for locale matching
    // (note: this is also a dependency of fluent-base)
    implementation("com.ibm.icu:icu4j:78.1")


// TODO:    add this line and test. if above is not present, use maven dependency
    //          implementation("net.xyzsd.fluent:fluent-base:**VERSION**")
}

tasks.test {
    useJUnitPlatform()
}