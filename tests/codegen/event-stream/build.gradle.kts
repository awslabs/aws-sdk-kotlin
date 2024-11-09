/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir

description = "AWS SDK for Kotlin codegen event stream integration test suite"

apply(from = rootProject.file("buildSrc/shared.gradle.kts"))

data class CodegenTest(
    val name: String,
    val model: Model,
    val serviceShapeId: String,
    val protocolName: String? = null,
)

data class Model(
    val fileName: String,
    val path: String = "src/commonTest/resources/",
) {
    val file: File
        get() = layout.projectDirectory.file(path + fileName).asFile
}

val tests = listOf(
    CodegenTest(
        "restJson1",
        Model("event-stream-model-template.smithy"),
        "aws.sdk.kotlin.test#TestService",
        "restJson1"
    ),
    CodegenTest(
        "awsJson11",
        Model("event-stream-initial-request-response.smithy"),
        "aws.sdk.kotlin.test#TestService",
        "awsJson1_1"
    ),
)

smithyBuild {
    tests.forEach { test ->
        projections.register(test.name) {
            imports = listOf(test.model.file.absolutePath)
            transforms = listOf(
                """
                {
                  "name": "includeServices",
                  "args": {
                    "services": ["${test.serviceShapeId}"]
                  }
                }
                """,
            )
            smithyKotlinPlugin {
                serviceShapeId = test.serviceShapeId
                packageName = "aws.sdk.kotlin.test.${test.name.lowercase()}"
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

kotlin {
    sourceSets {
        commonTest { // TODO: CHANGE TO JUST TEST WHEN NON-KMPing the project
            smithyBuild.projections.forEach {
                kotlin.srcDir(smithyBuild.smithyKotlinProjectionSrcDir(it.name))
            }
        }
    }
}

tasks.generateSmithyBuild {
    doFirst {
        tests.forEach { test -> fillInModel(test) }
    }
}

fun fillInModel(test: CodegenTest) {
    val modelFile = test.model.file
    val model = modelFile.readText()

    val opTraits =
        when (test.protocolName) {
            "restJson1", "restXml" -> """@http(method: "POST", uri: "/test-eventstream", code: 200)"""
            else -> ""
        }

    val replaced = model
        .replace("{protocol-name}", test.protocolName!!)
        .replace("{op-traits}", opTraits)

    modelFile.parentFile.mkdirs()
    modelFile.writeText(replaced)
}
