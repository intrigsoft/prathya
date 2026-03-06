package io.pratya.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

/**
 * Gradle task that runs pratya verifications.
 *
 * <p>Configure the task via the {@code pratya} extension or directly:
 *
 * <pre>{@code
 * pratya {
 *     classes = listOf("com.example.MyPactClass")
 *     failOnError = true
 * }
 * }</pre>
 */
public abstract class PratyaVerifyTask extends DefaultTask {

    @Input
    @Optional
    public abstract ListProperty<String> getClasses();

    @Input
    public abstract Property<Boolean> getFailOnError();

    @TaskAction
    public void verify() {
        List<String> classNames = getClasses().getOrElse(List.of());
        boolean shouldFail = getFailOnError().getOrElse(true);

        getLogger().lifecycle("Running pratya verifications...");

        int passCount = 0;
        int failCount = 0;

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                getLogger().lifecycle("  [PASS] Loaded pact class: {}", clazz.getName());
                passCount++;
            } catch (ClassNotFoundException e) {
                getLogger().error("  [FAIL] Cannot load class: {}", className);
                failCount++;
            }
        }

        getLogger().lifecycle("pratya verification complete: {} passed, {} failed.", passCount, failCount);

        if (failCount > 0 && shouldFail) {
            throw new GradleException(
                "pratya verification failed: " + failCount + " failure(s). "
                + "Set pratya { failOnError = false } to continue despite failures."
            );
        }
    }
}
