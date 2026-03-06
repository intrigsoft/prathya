plugins {
    `java-gradle-plugin`
    java
}

group = "io.pratya"
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
        create("pratyaPlugin") {
            id = "io.pratya"
            displayName = "Pratya Gradle Plugin"
            description = "Gradle plugin for running pratya verifications during the build lifecycle"
            implementationClass = "io.pratya.gradle.PratyaPlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
