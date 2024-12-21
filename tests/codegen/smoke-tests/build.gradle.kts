/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionPath
import aws.sdk.kotlin.shared.CodegenTest
import aws.sdk.kotlin.shared.Model

description = "AWS SDK for Kotlin's smoke test codegen test suite"

dependencies {
    jvmTestImplementation(gradleTestKit())
}

val tests = listOf(
    CodegenTest("successService", Model("smoke-tests-success.smithy"), "smithy.kotlin.traits#SuccessService"),
    CodegenTest("failureService", Model("smoke-tests-failure.smithy"), "smithy.kotlin.traits#FailureService"),
    CodegenTest("exceptionService", Model("smoke-tests-exception.smithy"), "smithy.kotlin.traits#ExceptionService"),
)

configureProjections()
configureTasks()

fun configureProjections() {
    smithyBuild {
        this@Build_gradle.tests.forEach { test ->
            projections.register(test.name) {
                imports = listOf(layout.projectDirectory.file(test.model.path + test.model.fileName).asFile.absolutePath)
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
}

fun configureTasks() {
    tasks.register("stageServices") {
        dependsOn(tasks.generateSmithyProjections)
        doLast {
            this@Build_gradle.tests.forEach { test ->
                val projectionPath = smithyBuild.smithyKotlinProjectionPath(test.name).get()
                val destinationPath = layout.projectDirectory.asFile.absolutePath + "/services/${test.name}"

                copy {
                    from("$projectionPath/src")
                    into("$destinationPath/generated-src")
                }

                copy {
                    from("$projectionPath/build.gradle.kts")
                    into(destinationPath)
                }
            }
        }
    }

    tasks.withType<Test> {
        dependsOn(tasks.getByName("stageServices"))
        mustRunAfter(tasks.getByName("stageServices"))
    }

    tasks.build {
        dependsOn(tasks.getByName("stageServices"))
        mustRunAfter(tasks.getByName("stageServices"))
    }

    tasks.clean {
        this@Build_gradle.tests.forEach { test ->
            delete("services/${test.name}")
        }
    }
}
