/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionPath

description = "Tests for smoke tests runners"

plugins {
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
    alias(libs.plugins.kotlin.jvm)
}

val projections = listOf(
    Projection("successService", "smoke-tests-success.smithy", "smithy.kotlin.traits#SuccessService"),
    Projection("failureService", "smoke-tests-failure.smithy", "smithy.kotlin.traits#FailureService"),
    Projection("exceptionService", "smoke-tests-exception.smithy", "smithy.kotlin.traits#ExceptionService"),
)

configureProject()
configureProjections()
configureTasks()

fun configureProject() {
    val codegen by configurations.getting

    dependencies {
        codegen(project(":codegen:aws-sdk-codegen"))
        codegen(libs.smithy.cli)
        codegen(libs.smithy.model)

        implementation(project(":codegen:aws-sdk-codegen"))
        implementation(libs.smithy.kotlin.codegen)

        testImplementation(libs.kotlin.test)
        testImplementation(gradleTestKit())
    }
}

fun configureProjections() {
    smithyBuild {
        val pathToSmithyModels = "src/test/resources/"

        this@Build_gradle.projections.forEach { projection ->
            projections.register(projection.name) {
                imports = listOf(layout.projectDirectory.file(pathToSmithyModels + projection.modelFile).asFile.absolutePath)
                smithyKotlinPlugin {
                    serviceShapeId = projection.serviceShapeId
                    packageName = "aws.sdk.kotlin.test"
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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(tasks.generateSmithyProjections)
        kotlinOptions.allWarningsAsErrors = false
    }
}

fun configureTasks() {
    tasks.register("stageServices") {
        dependsOn(tasks.generateSmithyProjections)

        doLast {
            this@Build_gradle.projections.forEach { projection ->
                val projectionPath = smithyBuild.smithyKotlinProjectionPath(projection.name).get()
                val destinationPath = layout.projectDirectory.asFile.absolutePath + "/services/${projection.name}"

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

    tasks.build {
        dependsOn(tasks.getByName("stageServices"))
        mustRunAfter(tasks.getByName("stageServices"))
    }

    tasks.clean {
        this@Build_gradle.projections.forEach { projection ->
            delete("services/${projection.name}")
        }
    }

    tasks.withType<Test> {
        dependsOn(tasks.getByName("stageServices"))
        mustRunAfter(tasks.getByName("stageServices"))

        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}

/**
 * Holds metadata about a smithy projection
 */
data class Projection(
    val name: String,
    val modelFile: String,
    val serviceShapeId: String,
)
