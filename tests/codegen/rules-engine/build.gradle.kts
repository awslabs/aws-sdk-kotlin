/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin

description = "AWS SDK for Kotlin codegen rules engine integration test suite"

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
    CodegenTest("operationContextParams", Model("operation-context-params.smithy"), "aws.sdk.kotlin.test#TestService")
)

smithyBuild {
    tests.forEach { test ->
        projections.register(test.name) {
            imports = listOf(test.model.file.absolutePath)
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
