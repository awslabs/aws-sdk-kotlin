/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle

import aws.sdk.kotlin.gradle.tasks.registerCodegenTasks
import org.gradle.api.Plugin
import org.gradle.api.Project

const val CODEGEN_EXTENSION_NAME = "codegen"

/**
 * This plugin handles:
 * - applying smithy plugins to the project to generate code
 * - providing a [CodegenTask] to generate Kotlin sources from their respective smithy models.
 */
class CodegenPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        configurePlugins()
        installExtension()
        registerCodegenTasks()
    }

    private fun Project.configurePlugins() {
        // unfortunately all of the tasks provided by smithy rely on the plugin extension, so it also needs applied
        plugins.apply("software.amazon.smithy")
        tasks.getByName("smithyBuildJar").enabled = false
    }

    private fun Project.installExtension(): CodegenExtension {
        return extensions.create(CODEGEN_EXTENSION_NAME, CodegenExtension::class.java, project)
    }
}
