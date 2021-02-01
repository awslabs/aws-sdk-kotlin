/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin

import org.gradle.api.Project
import software.amazon.smithy.gradle.tasks.SmithyBuild
import java.io.File
import java.nio.file.Files

/**
 * register the `generateSdk` task that `build` depends on
 */
fun Project.registerCodegenTasks() {

    val smithyBuildConfig = buildDir.resolve("smithy-build.json")

    // generate the projection file for smithy to consume
    val generateSmithyBuild = tasks.create("generateSmithyBuild") {
        description = "generate smithy-build.json"
        doFirst {
            buildDir.mkdir()
            smithyBuildConfig.writeText(generateSmithyBuild())
        }
    }

    val generatedSdkDir = projectDir.resolve("generated-sdk")

    // re-use the tasks defined by the smithy-gradle plugin
    val generateSdk = tasks.create("generateSdk", SmithyBuild::class.java) {
        description = "generate AWS service client from the model"
        addCompileClasspath = true
        smithyBuildConfigs = files(smithyBuildConfig)
        inputs.file(smithyBuildConfig)
        outputs.dir(generatedSdkDir.resolve("src"))
    }

    val cleanGeneratedSdkDir = {
        // copy the generated sdk to setup the source sets how we want
        if (generatedSdkDir.exists()) {
            generatedSdkDir.deleteRecursively()
        }
    }

    generateSdk.doLast {
        // TODO - add a flag to kotlin codegen plugin to just turn off the generation of build.gradle.kts
        val generatedBuildFile = projectionRootDir.resolve("build.gradle.kts")
        if (generatedBuildFile.exists()) {
            generatedBuildFile.delete()
        }

        cleanGeneratedSdkDir()
        generatedSdkDir.mkdir()
//        val ideaActive = System.getProperty("idea.active")?.toBoolean() ?: false
//        if (ideaActive) {
//            pluginManager.apply("idea")
//            val idea = plugins.getPlugin(org.gradle.plugins.ide.idea.IdeaPlugin::class.java)
//            idea.model.module.generatedSourceDirs = setOf(generatedSdkDir)
//        }
        Files.move(projectionRootDir.resolve("src").toPath(), generatedSdkDir.resolve("src").toPath())
    }

    generateSdk.dependsOn(generateSmithyBuild)

    // FIXME - use the Kotlin Gradle plugin directly when we enable MPP since the task names differ between
    //  kotlin("jvm") and kotlin("multiplatform") it will be easier to find all tasks by type rather than name
    tasks.getByName("compileKotlin").dependsOn(generateSdk)

    tasks.getByName("clean").doLast {
        cleanGeneratedSdkDir()
    }
}

/**
 * Get the root directory where the generated SDK is projected to
 *
 * e.g.
 * ```
 * build/smithyprojections/PROJECTION/PROJECTION/kotlin-codegen
 * ```
 */
val Project.projectionRootDir: File
    get() {
        val sdkMeta = generatedSdkMetadata()
        return buildDir.resolve("smithyprojections/${sdkMeta.projectionName}/${sdkMeta.projectionName}/kotlin-codegen")
    }

/**
 * Metadata about a generated service including info required for smithy-build.json
 */
data class SdkMetadata(
    val name: String,
    val moduleName: String,
    val moduleVersion: String = "1.0",
    val modelFile: File,
    val projectionName: String,
    val sdkId: String
)

fun Project.generatedSdkMetadata(): SdkMetadata {
    val sdkIdLower = awsServiceTrait.sdkId.split(" ").joinToString(separator = "") {
        it.toLowerCase()
    }

    return SdkMetadata(
        name = serviceShape.id.toString(),
        moduleName = "aws.sdk.kotlin.$sdkIdLower",
        modelFile = awsModelFile,
        projectionName = sdkIdLower,
        sdkId = awsServiceTrait.sdkId
    )
}

// Generates a smithy-build.json file by creating a new projection.
// The generated smithy-build.json file is not committed since
// it is rebuilt each time codegen is performed.
private fun Project.generateSmithyBuild(): String {
    val sdkMeta = generatedSdkMetadata()

    val projections = """
            "${sdkMeta.projectionName}": {
                "imports": ["${sdkMeta.modelFile.absolutePath}"],
                "plugins": {
                    "kotlin-codegen": {
                      "service": "${sdkMeta.name}",
                      "module": "${sdkMeta.moduleName}",
                      "moduleVersion": "${sdkMeta.moduleVersion}",
                      "sdkId": "${sdkMeta.sdkId}",
                      "build": {
                        "rootProject": false
                      }
                    }
                }
            }
    """

    return """
    {
        "version": "1.0",
        "projections": {
            $projections
        }
    }
    """.trimIndent()
}
