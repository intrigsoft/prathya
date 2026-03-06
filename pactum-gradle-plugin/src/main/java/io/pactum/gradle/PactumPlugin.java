package io.pactum.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

/**
 * The pactum Gradle plugin.
 *
 * <p>Apply this plugin to your project to enable pactum verification tasks:
 *
 * <pre>{@code
 * plugins {
 *     id("io.pactum") version "1.0.0-SNAPSHOT"
 * }
 * }</pre>
 *
 * <p>The plugin registers a {@link PactumVerifyTask} and, by default, wires it
 * into the {@code check} lifecycle task so that pact verification runs automatically
 * alongside your tests.
 */
public class PactumPlugin implements Plugin<Project> {

    public static final String PACTUM_EXTENSION_NAME = "pactum";
    public static final String VERIFY_TASK_NAME = "pactumVerify";

    @Override
    public void apply(Project project) {
        PactumExtension extension = project.getExtensions()
            .create(PACTUM_EXTENSION_NAME, PactumExtension.class);

        project.getTasks().register(VERIFY_TASK_NAME, PactumVerifyTask.class, task -> {
            task.setDescription("Verifies all pacts and requirements annotated in the project.");
            task.setGroup("verification");
            task.getClasses().set(extension.getClasses());
            task.getFailOnError().set(extension.getFailOnError());
        });

        // Wire pactumVerify into the standard 'check' lifecycle only when the
        // Java plugin (or another plugin that registers 'check') is present.
        project.getPlugins().withType(JavaPlugin.class, javaPlugin ->
            project.getTasks().named("check", check ->
                check.dependsOn(VERIFY_TASK_NAME)));
    }
}
