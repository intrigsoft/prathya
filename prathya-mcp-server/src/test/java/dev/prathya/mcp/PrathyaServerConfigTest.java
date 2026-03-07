package dev.prathya.mcp;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PrathyaServerConfigTest {

    @Test
    void parse_defaultContractFile() {
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{});
        assertEquals(Paths.get("CONTRACT.yaml"), config.getContractFile());
        assertNull(config.getTestClassesDir());
        assertNull(config.getClassesDir());
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
    void annotationScanClasspath_empty() {
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{});
        assertTrue(config.getAnnotationScanClasspath().isEmpty());
    }
}
