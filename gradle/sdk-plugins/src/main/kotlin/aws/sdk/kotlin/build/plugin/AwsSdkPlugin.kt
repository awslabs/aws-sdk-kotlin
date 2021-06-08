/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 *
 */

package aws.sdk.kotlin.build.plugin

import aws.sdk.kotlin.build.plugin.smithy.resolveSdkDependencies
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

/**
 * Custom gradle plugin for AWS services. This plugin handles:
 *
 * - setting up codegen tasks to generate the Kotlin sources from their respective smithy models. Compilation
 *   tasks are then configured to depend on these tasks.
 * - configuring dependencies for the generated SDK by inspecting the model and updating the project
 *
 * SDK projects are scaffolded by tooling to setup the `build.gradle.kts` for the project which relies on this
 * plugin. The convention expected by this plugin is to have a `model` directory which contains a Smithy
 * model named `service.json`.
 */
class AwsSdkPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        validateModel()
        configurePlugins()
        configureDependencies()
        registerCodegenTasks()
        setBuildMeta()
    }

    private fun Project.validateModel() {
        if (!awsModelFile.exists()) {
            throw AwsServicePluginException("model not found for project: $name")
        }
        logger.aws("found model file in: $awsModelFile")
    }

    private fun Project.configurePlugins() {
        // TODO - work with smithy team to make configuring this not require the Java plugin?
        plugins.apply("software.amazon.smithy")
        tasks.getByName("smithyBuildJar").enabled = false
    }

    private fun Project.configureDependencies() {
        // depend on aws-kotlin code generation
        dependencies.add("compileClasspath", project(":codegen:smithy-aws-kotlin-codegen"))

        // smithy plugin requires smithy-cli to be on the classpath, for whatever reason configuring the plugin
        // from this plugin doesn't work correctly so we explicitly set it
        val smithyVersion: String by project
        dependencies.add("compileClasspath", "software.amazon.smithy:smithy-cli:$smithyVersion")

        // add aws traits to the compile classpath so that the smithy build task can discover them
        dependencies.add("compileClasspath", "software.amazon.smithy:smithy-aws-traits:$smithyVersion")

        resolveSdkDependencies()
    }

    private fun Project.setBuildMeta() {
        description = serviceShape.getTrait(software.amazon.smithy.model.traits.TitleTrait::class.java).map { it.value }.orNull()
    }
}

class AwsServicePluginException(message: String) : GradleException(message)
