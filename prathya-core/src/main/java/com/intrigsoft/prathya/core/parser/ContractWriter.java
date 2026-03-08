package com.intrigsoft.prathya.core.parser;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.model.ModuleContract;

import java.nio.file.Path;

/**
 * Serialises a {@link ModuleContract} back to CONTRACT.yaml format.
 */
public interface ContractWriter {

    /**
     * Writes the contract to the given file path (overwrites if exists).
     */
    void write(ModuleContract contract, Path outputFile) throws PrathyaException;

    /**
     * Returns the contract as a YAML string without writing to disk.
     */
    String toYaml(ModuleContract contract) throws PrathyaException;
}
