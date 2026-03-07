plugins {
    java
    id("dev.prathya") version "1.0.0-SNAPSHOT"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("dev.prathya:prathya-annotations:1.0.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

prathya {
    failOnViolations.set(true)
}
