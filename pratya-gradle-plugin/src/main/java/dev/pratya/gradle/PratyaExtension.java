package dev.pratya.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class PratyaExtension {

    public abstract RegularFileProperty getContractFile();

    public abstract DirectoryProperty getClassesDir();

    public abstract DirectoryProperty getTestClassesDir();

    public abstract DirectoryProperty getOutputDir();

    public abstract Property<Boolean> getFailOnViolations();

    public abstract Property<Double> getMinRequirementCoverage();

    public abstract Property<Double> getMinCornerCaseCoverage();

    public abstract ListProperty<String> getExcludeStatuses();

    public abstract Property<Boolean> getFailOnTestFailure();

    public abstract Property<String> getRequirementId();

    public abstract Property<String> getStatusFilter();

    public abstract Property<String> getTestScope();

    public abstract ListProperty<String> getIntegrationTestPatterns();
}
