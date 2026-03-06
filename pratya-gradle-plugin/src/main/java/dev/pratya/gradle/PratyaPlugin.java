package dev.pratya.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class PratyaPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PratyaExtension extension = project.getExtensions()
                .create("pratya", PratyaExtension.class);

        // Set extension defaults
        extension.getContractFile().convention(
                project.getLayout().getProjectDirectory().file("CONTRACT.yaml"));
        extension.getClassesDir().convention(
                project.getLayout().getBuildDirectory().dir("classes/java/main"));
        extension.getTestClassesDir().convention(
                project.getLayout().getBuildDirectory().dir("classes/java/test"));
        extension.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("pratya"));
        extension.getFailOnViolations().convention(true);
        extension.getMinRequirementCoverage().convention(0.0);
        extension.getMinCornerCaseCoverage().convention(0.0);
        extension.getFailOnTestFailure().convention(true);
        extension.getStatusFilter().convention("approved");
        extension.getTestScope().convention("unit");

        // Register tasks and wire inputs from extension
        project.getTasks().register("pratyaVerify", PratyaVerifyTask.class, task -> {
            wireCommonProperties(task, extension);
            task.getFailOnViolations().convention(extension.getFailOnViolations());
            task.setDescription("Verify requirement coverage and fail on violations.");
            task.setGroup("verification");
        });

        project.getTasks().register("pratyaAudit", PratyaAuditTask.class, task -> {
            wireCommonProperties(task, extension);
            task.setDescription("Audit requirement coverage and log violations.");
            task.setGroup("verification");
        });

        project.getTasks().register("pratyaReport", PratyaReportTask.class, task -> {
            wireCommonProperties(task, extension);
            task.setDescription("Generate requirement coverage reports.");
            task.setGroup("reporting");
        });

        project.getTasks().register("pratyaRun", PratyaRunTask.class, task -> {
            wireCommonProperties(task, extension);
            task.getRequirementId().convention(extension.getRequirementId());
            task.getStatusFilter().convention(extension.getStatusFilter());
            task.getFailOnTestFailure().convention(extension.getFailOnTestFailure());
            task.getTestScopeName().convention(extension.getTestScope());
            task.getIntegrationTestPatterns().convention(extension.getIntegrationTestPatterns());
            task.getTestReportsDir().convention(
                    project.getLayout().getBuildDirectory().dir("test-results/test"));
            task.setDescription("Run tests for specific requirements and compute coverage.");
            task.setGroup("verification");
        });

        project.getTasks().register("pratyaAggregate", PratyaAggregateTask.class, task -> {
            task.getOutputDir().convention(
                    project.getLayout().getBuildDirectory().dir("pratya-aggregate"));
            task.setDescription("Aggregate coverage reports from subprojects.");
            task.setGroup("reporting");
        });

        // Wire pratyaVerify into 'check' lifecycle when Java plugin is applied
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            project.getTasks().named("check", check ->
                    check.dependsOn("pratyaVerify"));
            project.getTasks().named("pratyaVerify", task ->
                    task.dependsOn("test"));
            project.getTasks().named("pratyaReport", task ->
                    task.dependsOn("test"));
            project.getTasks().named("pratyaRun", task ->
                    task.dependsOn("test"));
        });

        // Wire aggregate task to collect reports from subprojects
        project.afterEvaluate(p -> {
            p.getTasks().named("pratyaAggregate", PratyaAggregateTask.class, aggTask -> {
                for (Project subproject : p.getSubprojects()) {
                    subproject.getPlugins().withId("dev.pratya", plugin -> {
                        subproject.getTasks().named("pratyaVerify", PratyaVerifyTask.class, verifyTask ->
                                aggTask.getReportFiles().from(
                                        verifyTask.getOutputDir().file("pratya-report.json")));
                        aggTask.dependsOn(subproject.getTasks().named("pratyaVerify"));
                    });
                }
            });
        });
    }

    private void wireCommonProperties(AbstractPratyaTask task, PratyaExtension extension) {
        task.getContractFile().convention(extension.getContractFile());
        task.getClassesDir().convention(extension.getClassesDir());
        task.getTestClassesDir().convention(extension.getTestClassesDir());
        task.getOutputDir().convention(extension.getOutputDir());
        task.getExcludeStatuses().convention(extension.getExcludeStatuses());
        task.getMinRequirementCoverage().convention(extension.getMinRequirementCoverage());
        task.getMinCornerCaseCoverage().convention(extension.getMinCornerCaseCoverage());
    }
}
