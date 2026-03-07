package dev.prathya.core.parser;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.model.ModuleContract;

import java.nio.file.Path;

/**
 * Parses a {@code CONTRACT.yaml} file into a {@link ModuleContract}.
 */
public interface RequirementParser {

    ModuleContract parse(Path requirementYaml) throws PrathyaException;
}
