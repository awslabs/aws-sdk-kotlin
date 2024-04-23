/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

interface CodegenPluginExtension {
    val codegenOutputDir: DirectoryProperty
}

class CodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create extension and set defaults
        val extension = project.extensions.create("opsCodegen", CodegenPluginExtension::class.java).apply {
            codegenOutputDir.convention(project.layout.buildDirectory.dir("generated/ops-codegen/commonMain"))
        }

        // Generate the code
        project.task("codegenMapperOperations") {
            outputs.dir(extension.codegenOutputDir)
            doLast {
                CodegenOrchestrator(extension.codegenOutputDir.get().asFile).execute()
            }
        }

        // Make sure that commonMain includes the codegen
        project.kotlinExtension.sourceSets["commonMain"].kotlin.srcDir(extension.codegenOutputDir)
    }
}
