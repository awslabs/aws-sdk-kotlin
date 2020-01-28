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
import java.util.LinkedHashSet;
import java.util.Set;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.impldep.org.eclipse.jgit.annotations.Nullable;
import software.amazon.smithy.model.traits.DynamicTrait;

/**
 * Gradle configuration settings for Smithy.
 */
public class SmithyExtension {

    private FileCollection smithyBuildConfigs;
    private String projection = "source";
    private Set<String> projectionSourceTags = new LinkedHashSet<>();
    private Set<String> tags = new LinkedHashSet<>();
    private boolean allowUnknownTraits;
    private File outputDirectory;
    private boolean fork;

    /**
     * Gets the projection name in use by the extension.
     *
     * @return Returns the projection name and defaults to "source".
     */
    public String getProjection() {
        return projection;
    }

    /**
     * Sets the projection name.
     *
     * <p>There must be a corresponding projection definition in the
     * {@code smithy-build.json} file of the project.
     *
     * @param projection Projection to set.
     */
    public void setProjection(String projection) {
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
        this.projectionSourceTags.clear();
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
        this.tags.clear();
        this.tags.addAll(tags);
    }

    /**
     * Gets a custom collection of smithy-build.json files to use when
     * building the model.
     *
     * @return Returns the collection of build configurations.
     */
    public @Nullable FileCollection getSmithyBuildConfigs() {
        return smithyBuildConfigs;
    }

    /**
     * Sets a custom collection of smithy-build.json files to use when
     * building the model.
     *
     * @param smithyBuildConfigs Sets the collection of build configurations.
     */
    public void setSmithyBuildConfigs(FileCollection smithyBuildConfigs) {
        this.smithyBuildConfigs = smithyBuildConfigs;
    }

    /**
     * Gets whether or not unknown traits in the model should be ignored.
     *
     * <p>By default, the build will fail if unknown traits are encountered.
     * This can be set to true to allow unknown traits to pass through the
     * model and be loaded as a {@link DynamicTrait}.
     *
     * @return Returns true if unknown traits are allowed.
     */
    public boolean getAllowUnknownTraits() {
        return allowUnknownTraits;
    }

    /**
     * Sets whether or not unknown traits are ignored.
     *
     * @param allowUnknownTraits Set to true to ignore unknown traits.
     */
    public void setAllowUnknownTraits(boolean allowUnknownTraits) {
        this.allowUnknownTraits = allowUnknownTraits;
    }

    /**
     * Gets whether or not to fork when running the Smithy CLI.
     *
     * <p>By default, the CLI is run in the same process as Gradle,
     * but inside of a thread with a custom class loader. This should
     * work in most cases, but there is an option to run inside of a
     * process if necessary.
     *
     * @return Returns true if the CLI should fork.
     */
    public boolean getFork() {
        return fork;
    }

    /**
     * Sets whether or not to fork when running the Smithy CLI.
     *
     * @param fork Set to true to fork when running the Smithy CLI.
     */
    public void setFork(boolean fork) {
        this.fork = fork;
    }

    /**
     * Sets the output directory of running Smithy Build.
     *
     * @param outputDirectory Output directory to set.
     */
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Gets the output directory for running Smithy build.
     *
     * @return Returns the output directory.
     */
    public @Nullable File getOutputDirectory() {
        return outputDirectory;
    }
}
