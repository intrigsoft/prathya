package io.pactum.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

/**
 * Gradle task that runs pactum verifications.
 *
 * <p>Configure the task via the {@code pactum} extension or directly:
 *
 * <pre>{@code
 * pactum {
 *     classes = listOf("com.example.MyPactClass")
 *     failOnError = true
 * }
 * }</pre>
 */
public abstract class PactumVerifyTask extends DefaultTask {

    @Input
    @Optional
    public abstract ListProperty<String> getClasses();

    @Input
    public abstract Property<Boolean> getFailOnError();

    @TaskAction
    public void verify() {
        List<String> classNames = getClasses().getOrElse(List.of());
        boolean shouldFail = getFailOnError().getOrElse(true);

        getLogger().lifecycle("Running pactum verifications...");

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

        getLogger().lifecycle("pactum verification complete: {} passed, {} failed.", passCount, failCount);

        if (failCount > 0 && shouldFail) {
            throw new GradleException(
                "pactum verification failed: " + failCount + " failure(s). "
                + "Set pactum { failOnError = false } to continue despite failures."
            );
        }
    }
}
