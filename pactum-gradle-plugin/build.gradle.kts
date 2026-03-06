plugins {
    `java-gradle-plugin`
    java
}

group = "io.pactum"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("pactumPlugin") {
            id = "io.pactum"
            displayName = "Pactum Gradle Plugin"
            description = "Gradle plugin for running pactum verifications during the build lifecycle"
            implementationClass = "io.pactum.gradle.PactumPlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
