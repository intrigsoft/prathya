package dev.pratya.core.parser;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.ModuleContract;

import java.nio.file.Path;

/**
 * Parses a {@code CONTRACT.yaml} file into a {@link ModuleContract}.
 */
public interface RequirementParser {

    ModuleContract parse(Path requirementYaml) throws PratyaException;
}
