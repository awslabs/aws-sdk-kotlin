/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.get

/**
 * Get the root directory of the generated kotlin code for a projection
 */
internal fun Project.projectionRootDir(projectionName: String): java.io.File =
    file("${project.buildDir}/smithyprojections/${project.name}/$projectionName/kotlin-codegen")

/**
 * Get the [CodegenExtension] instance configured for the project
 */
internal val Project.codegenExtension: CodegenExtension
    get() = ((this as ExtensionAware).extensions[CODEGEN_EXTENSION_NAME] as? CodegenExtension) ?: error("CodegenPlugin has not been applied")
