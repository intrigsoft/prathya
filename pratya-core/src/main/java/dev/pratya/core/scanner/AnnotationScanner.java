package dev.pratya.core.scanner;

import dev.pratya.core.model.TraceEntry;

import java.nio.file.Path;
import java.util.List;

/**
 * Scans compiled test class directories for {@code @Requirement} annotations.
 */
public interface AnnotationScanner {

    List<TraceEntry> scan(List<Path> testClassDirectories);
}
