package dev.pactum.core.parser;

import dev.pactum.core.PactumException;
import dev.pactum.core.model.ModuleContract;

import java.nio.file.Path;

/**
 * Parses a {@code REQUIREMENT.yaml} file into a {@link ModuleContract}.
 */
public interface RequirementParser {

    ModuleContract parse(Path requirementYaml) throws PactumException;
}
