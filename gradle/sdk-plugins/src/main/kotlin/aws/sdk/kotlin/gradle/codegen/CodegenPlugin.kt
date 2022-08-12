/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.codegen

import aws.sdk.kotlin.gradle.codegen.tasks.createSmithyCliConfiguration
import aws.sdk.kotlin.gradle.codegen.tasks.registerCodegenTasks
import org.gradle.api.Plugin
import org.gradle.api.Project

const val CODEGEN_EXTENSION_NAME = "codegen"

/**
 * This plugin handles:
 * - applying smithy-gradle-plugin to the project to generate code
 * - providing a tasks to generate Kotlin sources from their respective smithy models.
 */
class CodegenPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        configurePlugins()
        installExtension()
        registerCodegenTasks()
    }

    private fun Project.configurePlugins() {
        createSmithyCliConfiguration()
        // unfortunately all of the tasks provided by smithy rely on the plugin extension, so it also needs applied
        // see https://github.com/awslabs/smithy-gradle-plugin/issues/45
        plugins.apply("software.amazon.smithy")
        tasks.getByName("smithyBuildJar").enabled = false
    }

    private fun Project.installExtension(): CodegenExtension {
        return extensions.create(CODEGEN_EXTENSION_NAME, CodegenExtension::class.java, project)
    }
}
