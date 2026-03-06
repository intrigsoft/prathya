package io.pratya.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

/**
 * The pratya Gradle plugin.
 *
 * <p>Apply this plugin to your project to enable pratya verification tasks:
 *
 * <pre>{@code
 * plugins {
 *     id("io.pratya") version "1.0.0-SNAPSHOT"
 * }
 * }</pre>
 *
 * <p>The plugin registers a {@link PratyaVerifyTask} and, by default, wires it
 * into the {@code check} lifecycle task so that pact verification runs automatically
 * alongside your tests.
 */
public class PratyaPlugin implements Plugin<Project> {

    public static final String PRATYA_EXTENSION_NAME = "pratya";
    public static final String VERIFY_TASK_NAME = "pratyaVerify";

    @Override
    public void apply(Project project) {
        PratyaExtension extension = project.getExtensions()
            .create(PRATYA_EXTENSION_NAME, PratyaExtension.class);

        project.getTasks().register(VERIFY_TASK_NAME, PratyaVerifyTask.class, task -> {
            task.setDescription("Verifies all pacts and requirements annotated in the project.");
            task.setGroup("verification");
            task.getClasses().set(extension.getClasses());
            task.getFailOnError().set(extension.getFailOnError());
        });

        // Wire pratyaVerify into the standard 'check' lifecycle only when the
        // Java plugin (or another plugin that registers 'check') is present.
        project.getPlugins().withType(JavaPlugin.class, javaPlugin ->
            project.getTasks().named("check", check ->
                check.dependsOn(VERIFY_TASK_NAME)));
    }
}
