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
        group = "codegen"
        outputs.file(smithyBuildConfig)
        doFirst {
            buildDir.mkdir()
            smithyBuildConfig.writeText(generateSmithyBuild())
        }
    }

    val generatedSdkDir = projectDir.resolve("generated-src")

    // re-use the tasks defined by the smithy-gradle plugin
    val generateSdk = tasks.create("generateSdk", SmithyBuild::class.java) {
        description = "generate AWS service client from the model"
        group = "codegen"
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
        cleanGeneratedSdkDir()
        generatedSdkDir.mkdir()
//        val ideaActive = System.getProperty("idea.active")?.toBoolean() ?: false
//        if (ideaActive) {
//            pluginManager.apply("idea")
//            val idea = plugins.getPlugin(org.gradle.plugins.ide.idea.IdeaPlugin::class.java)
//            idea.model.module.generatedSourceDirs = setOf(generatedSdkDir)
//        }
        Files.move(projectionRootDir.resolve("src/main").toPath(), generatedSdkDir.resolve("main").toPath())
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
    // FIXME - this comes from the project settings
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

    // FIXME - reconcile with latest sdk build.gradle.kts settings (also process version and description)
    val projections = """
            "${sdkMeta.projectionName}": {
                "imports": ["${sdkMeta.modelFile.absolutePath}"],
                "plugins": {
                    "kotlin-codegen": {
                      "service": "${sdkMeta.name}",
                      "package": {
                          "name": "${sdkMeta.moduleName}",
                          "version": "${sdkMeta.moduleVersion}"
                      },
                      "sdkId": "${sdkMeta.sdkId}",
                      "build": {
                        "rootProject": false,
                        "generateDefaultBuildFiles": false
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
