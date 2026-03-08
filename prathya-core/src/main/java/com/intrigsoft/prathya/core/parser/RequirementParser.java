package com.intrigsoft.prathya.core.parser;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.model.ModuleContract;

import java.nio.file.Path;

/**
 * Parses a {@code CONTRACT.yaml} file into a {@link ModuleContract}.
 */
public interface RequirementParser {

    ModuleContract parse(Path requirementYaml) throws PrathyaException;
}
