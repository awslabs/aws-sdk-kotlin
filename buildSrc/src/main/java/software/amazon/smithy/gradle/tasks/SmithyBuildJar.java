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

package software.amazon.smithy.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;
import software.amazon.smithy.cli.BuildParameterBuilder;
import software.amazon.smithy.gradle.SmithyExtension;
import software.amazon.smithy.gradle.SmithyUtils;

/**
 * Builds JARs from Smithy models.
 *
 * <p>A package that does not use a {@code projection} is built using both the
 * runtimeClasspath and buildscript classpath. Smithy models defined in the
 * project are copied literally into the generated JAR (that is, models found
 * in model/ and src/main/smithy/). After generating the JAR, validation is run
 * using only the runtimeClasspath to ensure that the generated model is
 * correct and has configured its dependencies correctly.
 *
 * <p>A package that builds a projection executes Smithy's build process using
 * only the buildscript classpath and *does not* use the compileClasspath at
 * all. This serves to isolate downstream consumers of the package from
 * internal models, but it also means that dependencies may need to be
 * specified both in the buildscript and in the normal dependencies block.
 */
public class SmithyBuildJar extends BaseSmithyTask {

    private String projection;
    private Set<String> projectionSourceTags = new TreeSet<>();
    private Set<String> tags = new TreeSet<>();
    private FileCollection smithyBuildConfigs;
    private File outputDirectory;

    /**
     * Gets the output directory for running Smithy build.
     *
     * @return Returns the output directory.
     */
    @OutputDirectory
    public File getOutputDirectory() {
        return SmithyUtils.resolveOutputDirectory(outputDirectory, getProject());
    }

    /**
     * Sets the output directory of running {@code smithy build}.
     *
     * <p>This is the root directory where artifacts are written.
     *
     * @param outputDirectory Output directory to set.
     */
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Gets the smithy-build.json files set on the task.
     *
     * @return Returns the resolved collection of configurations.
     */
    @InputFiles
    @Optional
    public final FileCollection getSmithyBuildConfigs() {
        return java.util.Optional.ofNullable(smithyBuildConfigs)
                .orElseGet(() -> getProject().files("smithy-build.json"));
    }

    /**
     * Sets a collection of {@code smithy-build.json} files to use when
     * building the model.
     *
     * <p>These configuration files are combined together and can
     * cross-reference each other in things like {@code apply} transforms.
     *
     * @param smithyBuildConfigs Sets the collection of build configurations.
     */
    public final void setSmithyBuildConfigs(FileCollection smithyBuildConfigs) {
        this.smithyBuildConfigs = smithyBuildConfigs;
    }

    /**
     * Gets the name of the projection being applied in the build.
     *
     * <p>The "source" projection means that no modifications are made to the
     * models.
     *
     * @return Returns the name of the projection.
     */
    @Input
    public final String getProjection() {
        return projection == null ? "source" : projection;
    }

    /**
     * Sets the name of the projection being applied.
     *
     * @param projection Name of the projection to set.
     */
    public final void setProjection(String projection) {
        this.projection = projection;
    }

    /**
     * Get the tags that are searched for in classpaths when determining which
     * models are projected into the created JAR.
     *
     * <p>This plugin will look through the JARs in the buildscript classpath
     * to see if they contain a META-INF/MANIFEST.MF attribute named
     * "Smithy-Tags" that matches any of the given projection source tags.
     * The Smithy models found in each matching JAR are copied into the
     * JAR being projected. This allows a projection JAR to aggregate models
     * into a single JAR.
     *
     * @return Returns the tags. This will never return null.
     */
    @Input
    public final Set<String> getProjectionSourceTags() {
        return projectionSourceTags;
    }

    /**
     * Set the projection source tags.
     *
     * @param projectionSourceTags Tags to search for.
     * @see #getProjectionSourceTags()
     */
    public final void setProjectionSourceTags(Set<String> projectionSourceTags) {
        this.projectionSourceTags.addAll(projectionSourceTags);
    }

    /**
     * Get the tags that are added to the JAR.
     *
     * <p>These tags are placed in the META-INF/MANIFEST.MF attribute named
     * "Smithy-Tags" as a comma separated list. JARs with Smithy-Tags can be
     * queried when building projections so that the Smithy models found in
     * each matching JAR are placed into the projection JAR.
     *
     * @return Returns the Smithy-Tags values that will be added to the created JAR.
     */
    @Input
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Sets the tags that are added that the JAR manifest in "Smithy-Tags".
     *
     * @param tags Smithy-Tags to add to the JAR.
     * @see #getTags()
     */
    public void setTags(Set<String> tags) {
        this.tags.addAll(tags);
    }

    @OutputDirectory
    public File getSmithyResourceStagingDir() {
        return SmithyUtils.getSmithyResourceTempDir(getProject());
    }

    @TaskAction
    public void execute() {
        // Configure the task from the extension if things aren't already setup.
        SmithyExtension extension = SmithyUtils.getSmithyExtension(getProject());

        if (projection == null) {
            getLogger().debug("Setting SmithyBuildJar projection from extension: {}", extension.getProjection());
            setProjection(extension.getProjection());
        }

        // Merge tags from the extension into the task.
        projectionSourceTags.addAll(extension.getProjectionSourceTags());
        tags.addAll(extension.getTags());

        // Merge or overwrite?
        if (smithyBuildConfigs == null) {
            setSmithyBuildConfigs(extension.getSmithyBuildConfigs());
            getLogger().debug("Setting SmithyBuildJar smithyBuildConfigs from extension: {}", smithyBuildConfigs);
        }

        writeHeading("Running smithy build");

        // Always add the group, the group + ":" + name, and the group + ":" + name + ":" + version as tags.
        if (!getProject().getGroup().toString().isEmpty()) {
            tags.add(getProject().getGroup().toString());
            tags.add(getProject().getGroup() + ":" + getProject().getName());
            tags.add(getProject().getGroup() + ":" + getProject().getName() + ":" + getProject().getVersion());
            getLogger().debug("Adding built-in Smithy JAR tags: {}", tags);
        }

        // Clear out the directories when rebuilding.
        getProject().delete(getSmithyResourceStagingDir().getParentFile().getParentFile());

        BuildParameterBuilder builder = new BuildParameterBuilder();
        // Note: the runtime classpath extends from the compile classpath.
        builder.libClasspath(SmithyUtils.getClasspath(getProject(), RUNTIME_CLASSPATH).getAsPath());
        builder.buildClasspath(SmithyUtils.getBuildscriptClasspath(getProject()).getAsPath());
        builder.projectionSource(getProjection());
        builder.projectionSourceTags(getProjectionSourceTags());
        builder.allowUnknownTraits(getAllowUnknownTraits());

        if (getOutputDirectory() != null) {
            builder.output(getOutputDirectory().getAbsolutePath());
        }

        getSmithyBuildConfigs().forEach(config -> builder.addConfigIfExists(config.getAbsolutePath()));

        if (!getModels().isEmpty()) {
            builder.addSourcesIfExists(getModels().getFiles().stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList()));
        }

        builder.discover(true);

        BuildParameterBuilder.Result result = builder.build();
        Object[] jars = result.classpath.split(":");
        SmithyUtils.executeCli(getProject(), result.args, getProject().files(jars));

        // Copy generated files where they're needed and register source sets.
        try {
            createSourceSets(getOutputDirectory().toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (getProject().getTasks().getByName("jar").getEnabled()) {
            copyModelsToStaging();
            getProject().getTasks().withType(Jar.class, task -> {
                getLogger().info("Adding tags to manifest: {}", tags);
                Attributes attributes = task.getManifest().getAttributes();
                attributes.put("Smithy-Tags", String.join(", ", getTags()));
            });
        }
    }

    // Create source sets for every projection + plugin combination.
    private void createSourceSets(Path output) throws IOException {
        for (Path projection : getDirectories(output)) {
            String projectionName = output.relativize(projection).toString();

            // Add a source set for the entire projection.
            String projectionSourceSetName = "smithy_" + projectionName;
            getLogger().info("Creating Smithy projection source set: {}", projectionSourceSetName);
            SmithyUtils.getSourceSets(getProject()).create(projectionSourceSetName, sourceSet -> {
                sourceSet.resources(sds -> sds.srcDir(projection.toFile()));
            });

            // Add a source set for each plugin in the projection.
            for (Path plugin : getDirectories(projection)) {
                String pluginName = projection.relativize(plugin).toString();
                String pluginSourceSetName = projectionSourceSetName + "_" + pluginName;
                getLogger().info("Creating Smithy plugin source set: {}", pluginSourceSetName);
                SmithyUtils.getSourceSets(getProject()).create(pluginSourceSetName, sourceSet -> {
                    sourceSet.resources(sds -> sds.srcDir(plugin.toFile()));
                });
            }
        }
    }

    private List<Path> getDirectories(Path path) throws IOException {
        return Files.list(path).filter(Files::isDirectory).collect(Collectors.toList());
    }

    // Copy the "sources" plugin output to the generated resources directory.
    private void copyModelsToStaging() {
        Path sources = SmithyUtils.getProjectionPluginPath(getProject(), getProjection(), "sources");

        if (!Files.isDirectory(sources)) {
            throw new GradleException("Smithy projection `" + getProjection() + "` not found. "
                                      + "Is this projection defined in your smithy-build.json file?");
        }

        getProject().copy(c -> {
            c.from(sources.toFile());
            c.into(SmithyUtils.getSmithyResourceTempDir(getProject()));
        });
    }

    protected void writeHeading(String text) {
        if (!getProjection().equals("source")) {
            text += " using the `" + getProjection() + "` projection";
        }

        super.writeHeading(text);
    }
}
