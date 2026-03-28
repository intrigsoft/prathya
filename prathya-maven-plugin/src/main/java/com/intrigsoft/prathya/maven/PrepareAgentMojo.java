package com.intrigsoft.prathya.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import javax.inject.Inject;
import java.util.List;

/**
 * Resolves the JaCoCo agent jar and sets the {@code prathya.argLine} project property
 * so that Surefire can instrument classes into Prathya's own exec file.
 * <p>
 * Add {@code @{prathya.argLine}} to Surefire's {@code <argLine>} configuration to
 * enable coverage collection without requiring the user to configure the JaCoCo plugin.
 */
@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.INITIALIZE,
      threadSafe = true, requiresProject = true)
public class PrepareAgentMojo extends AbstractMojo {

    @Parameter(property = "prathya.jacocoVersion", defaultValue = "0.8.12")
    private String jacocoVersion;

    @Parameter(property = "prathya.execFile",
               defaultValue = "${project.build.directory}/prathya/jacoco/jacoco.exec")
    private String execFile;

    @Parameter(property = "prathya.propertyName", defaultValue = "prathya.argLine")
    private String propertyName;

    @Parameter(property = "prathya.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    @Inject
    private RepositorySystem repoSystem;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Prathya prepare-agent skipped.");
            return;
        }

        // Resolve org.jacoco:org.jacoco.agent:runtime:<jacocoVersion>
        DefaultArtifact artifact = new DefaultArtifact(
                "org.jacoco", "org.jacoco.agent", "runtime", "jar", jacocoVersion);
        ArtifactRequest request = new ArtifactRequest(artifact, remoteRepos, null);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(
                    "Could not resolve JaCoCo agent " + jacocoVersion + ": " + e.getMessage(), e);
        }

        String agentJar = result.getArtifact().getFile().getAbsolutePath();
        String agentArg = "-javaagent:" + agentJar + "=destfile=" + execFile + ",append=true";

        project.getProperties().setProperty(propertyName, agentArg);
        getLog().info("Prathya JaCoCo agent configured: " + agentArg);
        getLog().info("  Add @{" + propertyName + "} to maven-surefire-plugin <argLine> to enable coverage.");
    }
}
