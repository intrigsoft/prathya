package io.pactum.maven;

import io.pactum.core.PactumEngine;
import io.pactum.core.VerificationResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs pactum verifications for the project's annotated classes.
 * Bind this goal to the {@code verify} lifecycle phase to automatically
 * check pacts on every build.
 *
 * <pre>{@code
 * <plugin>
 *   <groupId>io.pactum</groupId>
 *   <artifactId>pactum-maven-plugin</artifactId>
 *   <version>1.0.0-SNAPSHOT</version>
 *   <executions>
 *     <execution>
 *       <goals><goal>verify</goal></goals>
 *     </execution>
 *   </executions>
 * </plugin>
 * }</pre>
 *
 * <p>The goal is automatically bound to the {@code verify} phase by default.
 * No explicit {@code <phase>} element is required unless you want to override that default.
 */
@Mojo(
    name = "verify",
    defaultPhase = LifecyclePhase.VERIFY,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    threadSafe = true
)
public class PactumVerifyMojo extends AbstractMojo {

    /**
     * Fully-qualified class names to scan for {@code @Pact} and {@code @Requirement} annotations.
     * If empty, pactum will attempt to scan the project's compile output directory.
     */
    @Parameter(property = "pactum.classes")
    private List<String> classes;

    /**
     * Set to {@code true} to skip pactum verification entirely.
     */
    @Parameter(property = "pactum.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Set to {@code true} to fail the build if any pact verification fails.
     */
    @Parameter(property = "pactum.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * The project's runtime classpath elements, injected by Maven so that the plugin
     * can load classes compiled from the project under verification.
     */
    @Parameter(defaultValue = "${project.runtimeClasspathElements}", readonly = true, required = true)
    private List<String> classpathElements;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("pactum verification skipped (pactum.skip=true).");
            return;
        }

        getLog().info("Running pactum verifications...");

        PactumEngine engine = new PactumEngine();
        Class<?>[] targetClasses = resolveClasses();
        VerificationResult result = engine.verify(targetClasses);

        logResults(result);

        if (!result.isSuccess() && failOnError) {
            throw new MojoFailureException(
                "pactum verification failed: " + result.getFailed().size() + " failure(s). "
                + "Set pactum.failOnError=false to continue the build despite failures."
            );
        }
    }

    private Class<?>[] resolveClasses() throws MojoExecutionException {
        if (classes == null || classes.isEmpty()) {
            return new Class<?>[0];
        }

        ClassLoader projectClassLoader = buildProjectClassLoader();
        Class<?>[] resolved = new Class<?>[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            try {
                resolved[i] = Class.forName(classes.get(i), true, projectClassLoader);
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException("Cannot load class: " + classes.get(i), e);
            }
        }
        return resolved;
    }

    private ClassLoader buildProjectClassLoader() throws MojoExecutionException {
        List<URL> urls = new ArrayList<>();
        if (classpathElements != null) {
            for (String element : classpathElements) {
                try {
                    urls.add(new File(element).toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException("Invalid classpath element: " + element, e);
                }
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    private void logResults(VerificationResult result) {
        result.getPassed().forEach(msg -> getLog().info("  [PASS] " + msg));
        result.getFailed().forEach(msg -> getLog().error("  [FAIL] " + msg));
        getLog().info(String.format(
            "pactum verification complete: %d passed, %d failed.",
            result.getPassed().size(),
            result.getFailed().size()
        ));
    }
}
