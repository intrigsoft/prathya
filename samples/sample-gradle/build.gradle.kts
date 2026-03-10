plugins {
    java
    id("com.intrigsoft.prathya") version "0.6.0"
}

group = "com.example"
version = "0.6.0"

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
    testImplementation("com.intrigsoft.prathya:prathya-annotations:0.6.0")
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

prathya {
    failOnViolations.set(true)
}
