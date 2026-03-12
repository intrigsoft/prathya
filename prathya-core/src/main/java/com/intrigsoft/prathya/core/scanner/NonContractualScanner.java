package com.intrigsoft.prathya.core.scanner;

import com.intrigsoft.prathya.core.model.NonContractualEntry;

import java.nio.file.Path;
import java.util.List;

/**
 * Scans compiled production class directories for {@code @NonContractual} annotations.
 */
public interface NonContractualScanner {

    List<NonContractualEntry> scan(List<Path> classDirectories);

    default List<NonContractualEntry> scan(List<Path> classDirectories, List<Path> additionalClasspath) {
        return scan(classDirectories);
    }
}
