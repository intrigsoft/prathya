package io.pactum.gradle;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension that configures the pactum Gradle plugin.
 *
 * <pre>{@code
 * pactum {
 *     classes = listOf("com.example.MyPactClass", "com.example.AnotherPactClass")
 *     failOnError = true
 * }
 * }</pre>
 */
public abstract class PactumExtension {

    /**
     * Fully-qualified class names to scan for {@code @Pact} and {@code @Requirement} annotations.
     */
    public abstract ListProperty<String> getClasses();

    /**
     * When {@code true} (the default), a verification failure causes the build to fail.
     */
    public abstract Property<Boolean> getFailOnError();
}
