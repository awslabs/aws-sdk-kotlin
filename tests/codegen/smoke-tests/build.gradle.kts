/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionPath
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir
import aws.sdk.kotlin.tests.codegen.CodegenTest
import aws.sdk.kotlin.tests.codegen.Model

description = "AWS SDK for Kotlin's smoke test codegen test suite"

val tests = listOf(
    CodegenTest("successService", Model("smoke-tests-success.smithy"), "smithy.kotlin.traits#SuccessService"),
    CodegenTest("failureService", Model("smoke-tests-failure.smithy"), "smithy.kotlin.traits#FailureService"),
    CodegenTest("exceptionService", Model("smoke-tests-exception.smithy"), "smithy.kotlin.traits#ExceptionService"),
)

smithyBuild {
    val basePackage = "aws.sdk.kotlin.test.codegen.smoketest"

    projections {
        tests.forEach { test ->
            create(test.name) {
                val modelPath = layout.projectDirectory.file(test.model.path + test.model.fileName).asFile.absolutePath
                imports = listOf(modelPath)

                smithyKotlinPlugin {
                    serviceShapeId = test.serviceShapeId
                    packageName = "$basePackage.${test.name}"
                    packageVersion = project.version.toString()
                    sdkId = test.name.replaceFirstChar { it.uppercaseChar() }
                    buildSettings {
                        generateDefaultBuildFiles = false
                        generateFullProject = false
                    }
                    apiSettings {
                        visibility = "internal"
                    }
                }
            }
        }
    }
}

kotlin.sourceSets.getByName("test") {
    smithyBuild.projections.forEach { projection ->
        // Add generated model to source set
        kotlin.srcDir(smithyBuild.smithyKotlinProjectionSrcDir(projection.name))

        // Add generated smoke tests to source set
        kotlin.srcDir(smithyBuild.smithyKotlinProjectionPath(projection.name).map { it.resolve("src/test/kotlin") })
    }
}
