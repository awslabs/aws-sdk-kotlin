/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.tasks

import aws.sdk.kotlin.gradle.KotlinCodegenProjection
import aws.sdk.kotlin.gradle.codegenExtension
import aws.sdk.kotlin.gradle.projectionRootDir
import aws.sdk.kotlin.gradle.withObjectMember
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import software.amazon.smithy.gradle.tasks.SmithyBuild
import software.amazon.smithy.model.node.Node

internal fun Project.registerCodegenTasks() {
    // generate the projection file for smithy to consume
    val smithyBuildConfig = buildDir.resolve("smithy-build.json")
    val generateSmithyBuild = tasks.register("kotlinCodegenGenerateBuildConfig") {
        description = "generate smithy-build.json"
        group = "codegen"

        // set an input property based on a hash of all the plugin settings to get this task's
        // up-to-date checks to work correctly (model files are already an input to the actual build task)
        val pluginSettingsHash = project.codegenExtension.projections.values.fold(0) { acc, projection ->
            acc + projection.pluginSettings.hashCode()
        }

        inputs.property("pluginSettingsHash", pluginSettingsHash)
        outputs.file(smithyBuildConfig)
        doFirst {
            if (smithyBuildConfig.exists()) {
                smithyBuildConfig.delete()
            }
        }
        doLast {
            buildDir.mkdir()
            val extension = project.codegenExtension
            smithyBuildConfig.writeText(generateSmithyBuild(extension.projections.values))
        }
    }

    val codegenConfig = createCodegenConfiguration()
    val buildTask = project.tasks.register<SmithyBuild>("kotlinCodegenSmithyBuild") {
        dependsOn(generateSmithyBuild)
        description = "generate code using smithy-kotlin"
        group = "codegen"
        classpath = codegenConfig
        smithyBuildConfigs = files(smithyBuildConfig)

        inputs.file(smithyBuildConfig)

        val extension = project.codegenExtension
        println("registering imports for kotlinCodegenSmithyBuild: ${extension.projections.keys.joinToString()}")
        // register the model file(s) (imports)
        val imports = extension.projections.values.flatMap { it.imports }
        imports.forEach { importPath ->
            val f = project.file(importPath)
            if (f.exists()) {
                if (f.isDirectory) inputs.dir(f) else inputs.file(f)
            }
        }

        // ensure smithy-aws-kotlin-codegen is up to date
        inputs.files(codegenConfig)

        extension.projections.keys.forEach { projectionName ->
            outputs.dir(project.projectionRootDir(projectionName))
        }
    }

    project.tasks.register<CodegenTask>("kotlinCodegen") {
        dependsOn(buildTask)
        description = "generate code for projections"
    }
}

/**
 * Generate the "smithy-build.json" defining the projection
 */
private fun generateSmithyBuild(projections: Collection<KotlinCodegenProjection>): String {
    val buildConfig = Node.objectNodeBuilder()
        .withMember("version", "1.0")
        .withObjectMember("projections") {
            projections.forEach { projection ->
                withMember(projection.name, projection.toNode())
            }
        }
        .build()

    return Node.prettyPrintJson(buildConfig)
}

// create a configuration (classpath) needed by the SmithyBuild task
private fun Project.createCodegenConfiguration(): Configuration {
    val codegenConfig = configurations.maybeCreate("codegenTaskConfiguration")

    dependencies {
        // depend on aws-kotlin code generation
        codegenConfig(project(":codegen:smithy-aws-kotlin-codegen"))

        // smithy plugin requires smithy-cli to be on the classpath, for whatever reason configuring the plugin
        // from this plugin doesn't work correctly so we explicitly set it
        val smithyVersion: String by project
        codegenConfig("software.amazon.smithy:smithy-cli:$smithyVersion")

        // add aws traits to the compile classpath so that the smithy build task can discover them
        codegenConfig("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    }

    return codegenConfig
}
