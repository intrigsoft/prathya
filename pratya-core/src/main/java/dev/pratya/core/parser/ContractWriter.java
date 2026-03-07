package dev.pratya.core.parser;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.ModuleContract;

import java.nio.file.Path;

/**
 * Serialises a {@link ModuleContract} back to CONTRACT.yaml format.
 */
public interface ContractWriter {

    /**
     * Writes the contract to the given file path (overwrites if exists).
     */
    void write(ModuleContract contract, Path outputFile) throws PratyaException;

    /**
     * Returns the contract as a YAML string without writing to disk.
     */
    String toYaml(ModuleContract contract) throws PratyaException;
}
