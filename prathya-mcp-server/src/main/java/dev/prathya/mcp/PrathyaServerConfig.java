package dev.prathya.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Parses CLI arguments for the Prathya MCP server.
 */
public class PrathyaServerConfig {

    private Path contractFile;
    private Path testClassesDir;
    private Path classesDir;

    private PrathyaServerConfig() {}

    public static PrathyaServerConfig parse(String[] args) {
        PrathyaServerConfig config = new PrathyaServerConfig();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--contract" -> {
                    if (++i < args.length) config.contractFile = Paths.get(args[i]);
                }
                case "--test-classes" -> {
                    if (++i < args.length) config.testClassesDir = Paths.get(args[i]);
                }
                case "--classes" -> {
                    if (++i < args.length) config.classesDir = Paths.get(args[i]);
                }
            }
        }

        if (config.contractFile == null) {
            config.contractFile = Paths.get("CONTRACT.yaml");
        }

        config.autoDetectDirectories();

        return config;
    }

    public Path getContractFile() { return contractFile; }
    public Path getTestClassesDir() { return testClassesDir; }
    public Path getClassesDir() { return classesDir; }

    /**
     * Returns the additional classpath entries for the annotation scanner,
     * combining classes dir and test-classes dir when available.
     */
    public List<Path> getAnnotationScanClasspath() {
        if (classesDir != null) {
            return List.of(classesDir);
        }
        return List.of();
    }

    /**
     * Auto-detects test-classes and classes directories from standard Maven/Gradle layouts
     * relative to the contract file's parent directory, if not explicitly configured.
     */
    private void autoDetectDirectories() {
        if (testClassesDir != null) {
            return;
        }
        Path contractParent = contractFile.toAbsolutePath().getParent();
        if (contractParent == null) {
            return;
        }

        // Try Maven layout
        Path mavenTestClasses = contractParent.resolve("target/test-classes");
        Path mavenClasses = contractParent.resolve("target/classes");
        if (Files.isDirectory(mavenTestClasses)) {
            testClassesDir = mavenTestClasses;
            if (classesDir == null && Files.isDirectory(mavenClasses)) {
                classesDir = mavenClasses;
            }
            return;
        }

        // Try Gradle layout
        Path gradleTestClasses = contractParent.resolve("build/classes/java/test");
        Path gradleClasses = contractParent.resolve("build/classes/java/main");
        if (Files.isDirectory(gradleTestClasses)) {
            testClassesDir = gradleTestClasses;
            if (classesDir == null && Files.isDirectory(gradleClasses)) {
                classesDir = gradleClasses;
            }
        }
    }
}
