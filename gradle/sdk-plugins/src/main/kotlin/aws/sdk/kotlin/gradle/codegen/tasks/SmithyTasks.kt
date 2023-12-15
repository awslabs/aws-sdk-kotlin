/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.codegen.tasks

import aws.sdk.kotlin.gradle.codegen.dsl.SmithyProjection
import aws.sdk.kotlin.gradle.codegen.dsl.codegenExtension
import aws.sdk.kotlin.gradle.codegen.dsl.withObjectMember
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import software.amazon.smithy.gradle.tasks.SmithyBuild
import software.amazon.smithy.model.node.Node

private const val GENERATE_SMITHY_BUILD_CONFIG_TASK_NAME = "generateSmithyBuildConfig"
private const val GENERATE_SMITHY_PROJECTIONS_TASK_NAME = "generateSmithyProjections"

internal fun Project.registerCodegenTasks() {
    // generate the projection file for smithy to consume
    val smithyBuildConfig = buildDir.resolve("smithy-build.json")
    val generateSmithyBuild = tasks.register(GENERATE_SMITHY_BUILD_CONFIG_TASK_NAME) {
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
    project.tasks.register<SmithyBuild>(GENERATE_SMITHY_PROJECTIONS_TASK_NAME) {
        dependsOn(generateSmithyBuild)
        description = "generate projections (code) using Smithy"
        group = "codegen"
        classpath = codegenConfig
        smithyBuildConfigs = files(smithyBuildConfig)

        // use the actual project build directory rather than the erroneous default smithy uses which defaults is
        // correct in the default case but doesn't respect the buildDir setting being changed
        outputDirectory = buildDir.resolve("smithyprojections/${project.name}")

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
    codegenConfig.extendsFrom(createSmithyCliConfiguration())

    dependencies {
        // depend on AWS SDK Kotlin code generation
        codegenConfig(project(":codegen:aws-sdk-codegen"))
    }

    return codegenConfig
}

internal fun Project.createSmithyCliConfiguration(): Configuration {
    // see: https://github.com/awslabs/smithy-gradle-plugin/blob/main/src/main/java/software/amazon/smithy/gradle/SmithyPlugin.java#L119
    val smithyCliConfig = configurations.maybeCreate("smithyCli")

    // FIXME - this is tightly coupled to our project
    val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    val smithyVersion = versionCatalog.findVersion("smithy-version").get()
    dependencies {
        // smithy plugin requires smithy-cli to be on the classpath, for whatever reason configuring the plugin
        // from this plugin doesn't work correctly so we explicitly set it
        smithyCliConfig("software.amazon.smithy:smithy-cli:$smithyVersion")

        // add aws traits to the compile classpath so that the smithy build task can discover them
        smithyCliConfig("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    }
    return smithyCliConfig
}
