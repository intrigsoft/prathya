package com.intrigsoft.prathya.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrathyaPluginTest {

    @Test
    void appliesPluginAndRegistersExtension() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("com.intrigsoft.prathya");

        PrathyaExtension extension = project.getExtensions().findByType(PrathyaExtension.class);
        assertNotNull(extension, "prathya extension should be registered");
    }

    @Test
    void registersAllTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("com.intrigsoft.prathya");

        assertNotNull(project.getTasks().findByName("prathyaVerify"));
        assertNotNull(project.getTasks().findByName("prathyaAudit"));
        assertNotNull(project.getTasks().findByName("prathyaReport"));
        assertNotNull(project.getTasks().findByName("prathyaRun"));
        assertNotNull(project.getTasks().findByName("prathyaAggregate"));
    }

    @Test
    void verifyTaskWiredToCheck() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("com.intrigsoft.prathya");

        Task checkTask = project.getTasks().findByName("check");
        assertNotNull(checkTask);
        assertTrue(checkTask.getDependsOn().stream()
                        .anyMatch(dep -> {
                            if (dep instanceof String) {
                                return "prathyaVerify".equals(dep);
                            }
                            if (dep instanceof org.gradle.api.tasks.TaskProvider) {
                                return "prathyaVerify".equals(((org.gradle.api.tasks.TaskProvider<?>) dep).getName());
                            }
                            return dep.toString().contains("prathyaVerify");
                        }),
                "check task should depend on prathyaVerify");
    }
}
