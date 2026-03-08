package com.intrigsoft.prathya.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PrathyaServerConfigTest {

    @Test
    void parse_defaultContractFile() {
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{});
        assertEquals(Paths.get("CONTRACT.yaml"), config.getContractFile());
        // testClassesDir and classesDir may be auto-detected if target/test-classes exists
    }

    @Test
    void parse_allArgs() {
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--contract", "/path/to/contract.yaml",
                "--test-classes", "/path/to/test-classes",
                "--classes", "/path/to/classes"
        });
        assertEquals(Path.of("/path/to/contract.yaml"), config.getContractFile());
        assertEquals(Path.of("/path/to/test-classes"), config.getTestClassesDir());
        assertEquals(Path.of("/path/to/classes"), config.getClassesDir());
    }

    @Test
    void parse_contractOnly() {
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--contract", "my-contract.yaml"
        });
        assertEquals(Paths.get("my-contract.yaml"), config.getContractFile());
    }

    @Test
    void annotationScanClasspath_withClasses() {
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--classes", "/path/to/classes"
        });
        assertEquals(1, config.getAnnotationScanClasspath().size());
    }

    @Test
    void annotationScanClasspath_noClasses(@TempDir Path tmp) throws IOException {
        // Point contract to a temp dir without any build output — no auto-detect
        Path contractFile = tmp.resolve("CONTRACT.yaml");
        Files.writeString(contractFile, "module:\n  id: TST\n  name: Test\n");
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--contract", contractFile.toString()
        });
        assertTrue(config.getAnnotationScanClasspath().isEmpty());
    }

    @Test
    void autoDetect_mavenLayout(@TempDir Path tmp) throws IOException {
        // Create Maven-style directories
        Files.createDirectories(tmp.resolve("target/test-classes"));
        Files.createDirectories(tmp.resolve("target/classes"));
        Path contractFile = tmp.resolve("CONTRACT.yaml");
        Files.writeString(contractFile, "module:\n  id: TST\n  name: Test\n");

        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--contract", contractFile.toString()
        });

        assertEquals(tmp.resolve("target/test-classes"), config.getTestClassesDir());
        assertEquals(tmp.resolve("target/classes"), config.getClassesDir());
    }

    @Test
    void autoDetect_gradleLayout(@TempDir Path tmp) throws IOException {
        // Create Gradle-style directories (no Maven dirs)
        Files.createDirectories(tmp.resolve("build/classes/java/test"));
        Files.createDirectories(tmp.resolve("build/classes/java/main"));
        Path contractFile = tmp.resolve("CONTRACT.yaml");
        Files.writeString(contractFile, "module:\n  id: TST\n  name: Test\n");

        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--contract", contractFile.toString()
        });

        assertEquals(tmp.resolve("build/classes/java/test"), config.getTestClassesDir());
        assertEquals(tmp.resolve("build/classes/java/main"), config.getClassesDir());
    }

    @Test
    void autoDetect_skippedWhenExplicit() throws IOException {
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--test-classes", "/explicit/path"
        });

        // Should keep the explicit path, not auto-detect
        assertEquals(Path.of("/explicit/path"), config.getTestClassesDir());
    }

    @Test
    void autoDetect_noDirsExist(@TempDir Path tmp) throws IOException {
        Path contractFile = tmp.resolve("CONTRACT.yaml");
        Files.writeString(contractFile, "module:\n  id: TST\n  name: Test\n");

        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--contract", contractFile.toString()
        });

        // No target/ or build/ exists, so should remain null
        assertNull(config.getTestClassesDir());
    }
}
