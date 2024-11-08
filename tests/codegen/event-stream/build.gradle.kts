/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir

description = "Event stream codegen integration test suite"

data class Test(
    val projectionName: String,
    val protocolName: String,
    val modelTemplate: File,
) {
    val model: File
        get() = layout.buildDirectory.file("$projectionName/model.smithy").get().asFile
}

val tests = listOf(
    Test("restJson1", "restJson1", file("event-stream-model-template.smithy")),
    Test("awsJson11", "awsJson1_1", file("event-stream-initial-request-response.smithy")),
)

fun fillInModel(output: File, protocolName: String, template: File) {
    val input = template.readText()
    val opTraits = when (protocolName) {
        "restJson1", "restXml" -> """@http(method: "POST", uri: "/test-eventstream", code: 200)"""
        else -> ""
    }
    val replaced = input
        .replace("{protocol-name}", protocolName)
        .replace("{op-traits}", opTraits)

    output.parentFile.mkdirs()
    output.writeText(replaced)
}

val testServiceShapeId = "aws.sdk.kotlin.test#TestService"
smithyBuild {
    tests.forEach { test ->

        projections.register(test.projectionName) {
            imports = listOf(test.model.absolutePath)
            transforms = listOf(
                """
                {
                  "name": "includeServices",
                  "args": {
                    "services": ["$testServiceShapeId"]
                  }
                }
                """,
            )

            smithyKotlinPlugin {
                serviceShapeId = testServiceShapeId
                packageName = "aws.sdk.kotlin.test.${test.projectionName.lowercase()}"
                packageVersion = "1.0"
                buildSettings {
                    generateFullProject = false
                    generateDefaultBuildFiles = false
                    optInAnnotations = listOf(
                        "aws.smithy.kotlin.runtime.InternalApi",
                        "aws.sdk.kotlin.runtime.InternalSdkApi",
                    )
                }
            }
        }
    }
}

tasks.generateSmithyBuild {
    doFirst {
        tests.forEach { test -> fillInModel(test.model, test.protocolName, test.modelTemplate) }
    }
}

kotlin {
    sourceSets {
        commonTest {
            smithyBuild.projections.forEach {
                kotlin.srcDir(smithyBuild.smithyKotlinProjectionSrcDir(it.name))
            }
        }
    }
}
