package dev.pratya.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PratyaPluginTest {

    @Test
    void appliesPluginAndRegistersExtension() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("dev.pratya");

        PratyaExtension extension = project.getExtensions().findByType(PratyaExtension.class);
        assertNotNull(extension, "pratya extension should be registered");
    }

    @Test
    void registersAllTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("dev.pratya");

        assertNotNull(project.getTasks().findByName("pratyaVerify"));
        assertNotNull(project.getTasks().findByName("pratyaAudit"));
        assertNotNull(project.getTasks().findByName("pratyaReport"));
        assertNotNull(project.getTasks().findByName("pratyaRun"));
        assertNotNull(project.getTasks().findByName("pratyaAggregate"));
    }

    @Test
    void verifyTaskWiredToCheck() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("dev.pratya");

        Task checkTask = project.getTasks().findByName("check");
        assertNotNull(checkTask);
        assertTrue(checkTask.getDependsOn().stream()
                        .anyMatch(dep -> {
                            if (dep instanceof String) {
                                return "pratyaVerify".equals(dep);
                            }
                            if (dep instanceof org.gradle.api.tasks.TaskProvider) {
                                return "pratyaVerify".equals(((org.gradle.api.tasks.TaskProvider<?>) dep).getName());
                            }
                            return dep.toString().contains("pratyaVerify");
                        }),
                "check task should depend on pratyaVerify");
    }
}
