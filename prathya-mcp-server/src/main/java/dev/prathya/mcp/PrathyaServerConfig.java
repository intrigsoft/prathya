package dev.prathya.mcp;

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
}
