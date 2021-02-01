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

class AwsServicePlugin : Plugin<Project> {
    private val AWS_SERVICE_EXTENSION_NAME = "awsService"
    override fun apply(target: Project) = target.run {
        validateModel()
        configurePlugins()
        configureDependencies()
        registerCodegenTasks()

        val extension = installExtension()
    }

    private fun Project.installExtension(): AwsServiceExtension {
        return extensions.create(AWS_SERVICE_EXTENSION_NAME, AwsServiceExtension::class.java, project)
    }

    private fun Project.validateModel() {
        logger.aws("looking for model file in: $awsModelFile")
        if (!awsModelFile.exists()) {
            throw AwsServicePluginException("model not found for project: $name")
        }
    }

    private fun Project.configurePlugins() {
        // TODO - work with smithy team to make configuring this not require the Java plugin
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
}

class AwsServicePluginException(message: String) : GradleException(message)
