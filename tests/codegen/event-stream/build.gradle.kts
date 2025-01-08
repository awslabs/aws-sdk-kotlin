/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
}

description = "Event stream codegen integration test suite"

data class EventStreamTest(
    val projectionName: String,
    val protocolName: String,
    val modelTemplate: File,
) {
    val model: File
        get() = layout.buildDirectory.file("$projectionName/model.smithy").get().asFile
}

val tests = listOf(
    EventStreamTest("restJson1", "restJson1", file("event-stream-model-template.smithy")),
    EventStreamTest("awsJson11", "awsJson1_1", file("event-stream-initial-request-response.smithy")),
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

val testServiceShapeId = "aws.sdk.kotlin.test.eventstream#TestService"
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
                packageName = "aws.sdk.kotlin.test.eventstream.${test.projectionName.lowercase()}"
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

val codegen by configurations.getting
dependencies {
    codegen(project(":codegen:aws-sdk-codegen"))
    codegen(libs.smithy.cli)
    codegen(libs.smithy.model)
}

tasks.generateSmithyBuild {
    doFirst {
        tests.forEach { test -> fillInModel(test.model, test.protocolName, test.modelTemplate) }
    }
}

tasks.generateSmithyProjections {
    doFirst {
        // ensure the generated tests use the same version of the runtime as the aws aws-runtime
        val smithyKotlinRuntimeVersion = libs.versions.smithy.kotlin.runtime.version.get()
        System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinRuntimeVersion)

        val smithyKotlinCoroutinesVersion = libs.versions.coroutines.version.get()
        System.setProperty("smithy.kotlin.codegen.kotlinCoroutinesVersion", smithyKotlinCoroutinesVersion)
    }
}

val optinAnnotations = listOf(
    "kotlin.RequiresOptIn",
    "aws.smithy.kotlin.runtime.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
)
kotlin.sourceSets.all {
    optinAnnotations.forEach { languageSettings.optIn(it) }
}

kotlin.sourceSets.getByName("test") {
    smithyBuild.projections.forEach {
        kotlin.srcDir(smithyBuild.smithyKotlinProjectionSrcDir(it.name))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(tasks.generateSmithyProjections)
    // generated clients have quite a few warnings
    kotlinOptions.allWarningsAsErrors = false
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.smithy.kotlin.smithy.test)
    testImplementation(libs.smithy.kotlin.aws.signing.default)
    testImplementation(libs.smithy.kotlin.telemetry.api)

    // have to manually add all the dependencies of the generated client(s)
    // doing it this way (as opposed to doing what we do for protocol-tests) allows
    // the tests to work without a publish to maven-local step at the cost of maintaining
    // this set of dependencies manually
    // <-- BEGIN GENERATED DEPENDENCY LIST -->
    implementation(libs.bundles.smithy.kotlin.service.client)
    implementation(libs.smithy.kotlin.aws.event.stream)
    implementation(project(":aws-runtime:aws-http"))
    implementation(libs.smithy.kotlin.aws.json.protocols)
    implementation(libs.smithy.kotlin.serde.json)
    api(project(":aws-runtime:aws-config"))
    api(project(":aws-runtime:aws-core"))
    api(project(":aws-runtime:aws-endpoint"))
    // <-- END GENERATED DEPENDENCY LIST -->
}
