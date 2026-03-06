package dev.pratya.core.scanner;

import dev.pratya.core.model.TraceEntry;

import java.nio.file.Path;
import java.util.List;

/**
 * Scans compiled test class directories for {@code @Requirement} annotations.
 */
public interface AnnotationScanner {

    List<TraceEntry> scan(List<Path> testClassDirectories);

    /**
     * Scans test class directories for annotations, with additional classpath
     * entries available for class resolution (e.g. production classes).
     */
    default List<TraceEntry> scan(List<Path> testClassDirectories, List<Path> additionalClasspath) {
        return scan(testClassDirectories);
    }
}
