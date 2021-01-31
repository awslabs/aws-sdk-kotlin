/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin

import org.gradle.api.Project
import software.amazon.smithy.gradle.tasks.SmithyBuild
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.Model
import java.io.File
import kotlin.streams.toList

/**
 * register the `generateSdk` task that `build` depends on
 */
fun Project.registerCodegenTasks() {

    // re-use the tasks defined by the smithy-gradle plugin
    val generateSdk = tasks.create("generateSdk", SmithyBuild::class.java) {
        description = "generate AWS service client from the model"
        addCompileClasspath = true
        addRuntimeClasspath = true
        // TODO - move the smithy-build.json to buildDir?
        // TODO - set output dir for generated SDK
        this.outputDirectory
    }

    // generate the projection file for smithy to consume
    val generateSmithyBuild = tasks.create("generateSmithyBuild") {
        description = "generate smithy-build.json"
        doFirst {
            projectDir.resolve("smithy-build.json").writeText(generateSmithyBuild())
        }
    }

    generateSdk.dependsOn(generateSmithyBuild)

    tasks.getByName("build").dependsOn(generateSdk)
}

// Represents information needed to generate a smithy projection JSON stanza
data class AwsService(
    val name: String,
    val moduleName: String,
    val moduleVersion: String = "1.0",
    val modelFile: File,
    val projectionName: String,
    val sdkId: String
)

// Generates a smithy-build.json file by creating a new projection.
// The generated smithy-build.json file is not committed since
// it is rebuilt each time codegen is performed.
private fun Project.generateSmithyBuild(): String {
    val model = Model.assembler().addImport(awsModelFile.absolutePath).assemble().result.get()
    val services: List<ServiceShape> = model.shapes(ServiceShape::class.java).toList()
    require(services.size == 1) { "Expected one service per aws model, but found ${services.size} in ${awsModelFile.absolutePath}" }
    val serviceShape = services.first()
    val serviceApi = serviceShape.getTrait(software.amazon.smithy.aws.traits.ServiceTrait::class.java).orNull()
        ?: error { "Expected aws.api#service trait attached to model ${awsModelFile.absolutePath}" }

    val sdkIdLower = serviceApi.sdkId.split(" ").map {
        it.toLowerCase()
    }.joinToString()

    val service = AwsService(
        name = serviceShape.id.toString(),
        moduleName = "aws.sdk.kotlin.$sdkIdLower",
        modelFile = awsModelFile,
        projectionName = sdkIdLower,
        sdkId = serviceApi.sdkId
    )

    val projections = """
            "${service.projectionName}": {
                "imports": ["${service.modelFile.absolutePath}"],
                "plugins": {
                    "kotlin-codegen": {
                      "service": "${service.name}",
                      "module": "${service.moduleName}",
                      "moduleVersion": "${service.moduleVersion}",
                      "sdkId": "${service.sdkId}",
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

