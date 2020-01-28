/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.gradle;

import java.io.File;
import java.util.List;
import java.util.Objects;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import software.amazon.smithy.gradle.tasks.SmithyBuildJar;
import software.amazon.smithy.gradle.tasks.Validate;
import software.amazon.smithy.utils.ListUtils;

/**
 * Applies the Smithy plugin to Gradle.
 */
public final class SmithyPlugin implements Plugin<Project> {

    private static final String DEFAULT_CLI_VERSION = "0.9.7";
    private static final List<String> SOURCE_DIRS = ListUtils.of(
            "model", "src/$name/smithy", "src/$name/resources/META-INF/smithy");

    @Override
    public void apply(Project project) {
        // Ensure that the Java plugin is applied.
        project.getPluginManager().apply(JavaPlugin.class);

        project.getConfigurations().create("smithy");

        // Register the Smithy extension so that software.amazon.smithy.gradle.tasks can be configured.
        SmithyExtension extension = project.getExtensions().create("smithy", SmithyExtension.class);

        // Register the smithyValidate task.
        TaskProvider<Validate> validateProvider = project.getTasks()
                .register("smithyValidate", Validate.class);

        // Register the "smithyBuildJar" task. It's configured once the extension is available.
        TaskProvider<SmithyBuildJar> buildJarProvider = project.getTasks()
                .register("smithyBuildJar", SmithyBuildJar.class);

        validateProvider.configure(validateTask -> {
            validateTask.dependsOn("jar");
            validateTask.dependsOn(buildJarProvider);
            Task jarTask = project.getTasks().getByName("jar");
            // Only run the validate task if the jar task is enabled and did work.
            validateTask.setEnabled(jarTask.getEnabled());
            validateTask.onlyIf(t -> jarTask.getState().getDidWork());
            // Use only model discovery with the built JAR + the runtime classpath when validating.
            validateTask.setAddRuntimeClasspath(true);
            validateTask.setClasspath(jarTask.getOutputs().getFiles());
            // Set to an empty collection to ensure it doesn't include the source models in the project.
            validateTask.setModels(project.files());
            validateTask.setAllowUnknownTraits(extension.getAllowUnknownTraits());
        });

        buildJarProvider.configure(buildJarTask -> {
            registerSourceSets(project);
            addCliDependencies(project);
            buildJarTask.setAllowUnknownTraits(extension.getAllowUnknownTraits());
            // We need to manually add these files as a dependency because we
            // dynamically inject the Smithy CLI JARs into the classpath.
            Configuration smithyCliFiles = project.getConfigurations().getByName("smithyCli");
            buildJarTask.getInputs().files(smithyCliFiles);
        });

        project.getTasks().getByName("processResources").dependsOn(buildJarProvider);
        project.getTasks().getByName("test").dependsOn(validateProvider);
    }

    /**
     * Add the CLI to the "smithyCli" dependencies.
     *
     * <p>The Smithy CLI is invoked in various ways to perform a build. This
     * method ensures that the JARs needed to invoke the CLI with a custom
     * classpath include the JARs needed to run the CLI.
     *
     * <p>The plugin will attempt to detect which version of the Smithy
     * CLI to use when performing validation of the generated JAR. If a
     * CLI version is explicitly defined in the "smithyCli" configuration,
     * then that version is used. Otherwise, a CLI version is inferred by
     * scanning the compileClasspath and runtimeClasspath for instances of
     * the "smithy-model" dependency. If found, the version of smithy-model
     * detected is used for the CLI.
     *
     * @param project Project to add dependencies to.
     */
    private void addCliDependencies(Project project) {
        Configuration cli;
        if (project.getConfigurations().findByName("smithyCli") != null) {
            cli = project.getConfigurations().getByName("smithyCli");
        } else {
            cli = project.getConfigurations().create("smithyCli")
                    .extendsFrom(project.getConfigurations().getByName("runtimeClasspath"));
        }

        // Prefer explicitly set dependency first.
        DependencySet existing = cli.getAllDependencies();

        if (existing.stream().anyMatch(d -> isMatchingDependency(d, "smithy-cli"))) {
            project.getLogger().warn("(using explicitly configured Smithy CLI)");
            return;
        }

        failIfRunningInMainSmithyRepo(project);
        String cliVersion = detectCliVersionInDependencies(SmithyUtils.getClasspath(project, "runtimeClasspath"));

        if (cliVersion != null) {
            project.getLogger().warn("(detected Smithy CLI version {})", cliVersion);
        } else {
            // Finally, just guess the default version that the Gradle plugin knows about
            cliVersion = DEFAULT_CLI_VERSION;
            project.getLogger().warn("(assuming Smithy CLI version {})", DEFAULT_CLI_VERSION);
        }

        project.getDependencies().add(cli.getName(), "software.amazon.smithy:smithy-cli:" + cliVersion);
    }

    // Subprojects in the main Smithy repo must define an explicit smithy-cli dependency.
    // This is mainly because I couldn't figure out how to add a project dependency.
    private void failIfRunningInMainSmithyRepo(Project project) {
        if (project.getParent() != null) {
            Project parent = project.getParent();
            if (parent.getGroup().equals("software.amazon.smithy")) {
                for (Project subproject : parent.getSubprojects()) {
                    if (subproject.getPath().equals(":smithy-cli")) {
                        throw new GradleException("Detected that this is the main Smithy repo. "
                                                  + "You need to add an explicit :project dependency on :smithy-cli");
                    }
                }
            }
        }
    }

    // Check if there's a dependency on smithy-model somewhere, and assume that version.
    private String detectCliVersionInDependencies(Configuration configuration) {
        return configuration.getAllDependencies().stream()
                .filter(d -> isMatchingDependency(d, "smithy-model"))
                .map(Dependency::getVersion)
                .findFirst()
                .orElse(null);
    }

    private static boolean isMatchingDependency(Dependency dependency, String name) {
        return Objects.equals(dependency.getGroup(), "software.amazon.smithy") && dependency.getName().equals(name);
    }

    /**
     * Adds Smithy model files to the Java resources source sets of main and test.
     *
     * <p>Smithy models can be placed in {@code model/}, {@code src/main/smithy},
     * {@code src/test/smithy}, {@code src/main/resources/META-INF/smithy}, and
     * {@code src/test/resources/META-INF/smithy}. This code will add these
     * directories to the appropriate existing Java source sets as extensions.
     * Access to these source sets is provided by
     * {@link SmithyUtils#getSmithyModelSources}.
     *
     * <p>Additionally, this code adds the "manifest" plugin output of smithy
     * build to the "main" source set's resources, making them show up in the
     * generated JAR.
     *
     * @param project Project to modify.
     */
    private void registerSourceSets(Project project) {
        // Add the smithy source set to all Java source sets.
        // By default, look in model/ and src/main/smithy, src/test/smithy.
        for (SourceSet sourceSet : project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()) {
            String name = sourceSet.getName();
            SourceDirectorySet sds = project.getObjects().sourceDirectorySet(name, name + " Smithy sources");
            sourceSet.getExtensions().add("smithy", sds);
            SOURCE_DIRS.forEach(sourceDir -> sds.srcDir(sourceDir.replace("$name", name)));
            project.getLogger().debug("Adding Smithy extension to {} Java convention", name);

            // Include Smithy models and the generated manifest in the JAR.
            if (name.equals("main")) {
                File metaInf = SmithyUtils.getSmithyResourceTempDir(project).getParentFile().getParentFile();
                project.getLogger().debug("Registering Smithy resource artifacts with Java resources: {}", metaInf);
                sourceSet.getResources().srcDir(metaInf);
            }
        }
    }
}
