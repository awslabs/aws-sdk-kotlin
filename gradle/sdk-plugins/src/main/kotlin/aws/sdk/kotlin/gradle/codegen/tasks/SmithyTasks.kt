/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.codegen.tasks

import aws.sdk.kotlin.gradle.codegen.dsl.SmithyProjection
import aws.sdk.kotlin.gradle.codegen.dsl.codegenExtension
import aws.sdk.kotlin.gradle.codegen.dsl.withObjectMember
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

        // set an input property based on a hash of all the projections to get this task's
        // up-to-date checks to work correctly (model files are configured as an input to the actual build task)
        val projectionHash = project.objects.property(Int::class.java)
        projectionHash.set(0)
        project.codegenExtension.projections.all {
            projectionHash.set(projectionHash.get() + hashCode())
        }
        inputs.property("projectionHash", projectionHash)
        outputs.file(smithyBuildConfig)
        doFirst {
            if (smithyBuildConfig.exists()) {
                smithyBuildConfig.delete()
            }
        }
        doLast {
            buildDir.mkdir()
            val extension = project.codegenExtension
            val projections = extension.projections.asMap
            smithyBuildConfig.writeText(generateSmithyBuild(projections.values))
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

        // every time a projection is added wire up the imports and outputs appropriately for this task
        extension.projections.all {
            imports.forEach { importPath ->
                val f = project.file(importPath)
                if (f.exists()) {
                    if (f.isDirectory) inputs.dir(f) else inputs.file(f)
                }
            }
            outputs.dir(projectionRootDir)
        }

        // ensure smithy-aws-kotlin-codegen is up to date
        inputs.files(codegenConfig)
    }

    project.tasks.register<CodegenTask>("kotlinCodegen") {
        dependsOn(buildTask)
        description = "generate code for projections"
    }
}

/**
 * Generate the "smithy-build.json" defining the projection
 */
private fun generateSmithyBuild(projections: Collection<SmithyProjection>): String {
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
