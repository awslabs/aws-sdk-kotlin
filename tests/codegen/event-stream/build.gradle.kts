/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import software.amazon.smithy.gradle.tasks.SmithyBuild

plugins {
    kotlin("jvm")
    id("aws.sdk.kotlin.codegen")
}

description = "Event stream codegen integration test suite"

val smithyVersion: String by project
dependencies {
    implementation(project(":codegen:smithy-aws-kotlin-codegen"))
}

data class EventStreamTest(
    val projectionName: String,
    val protocolName: String,
) {
    val model: File
        get() = buildDir.resolve("${projectionName}/model.smithy")
}

val tests = listOf(
    EventStreamTest("restJson1", "restJson1")
)

fun fillInModel(output: File, protocolName: String) {
    val template = file("event-stream-model-template.smithy")
    val input = template.readText()
    val opTraits = when(protocolName) {
        "restJson1", "restXml" -> """@http(method: "POST", uri: "/test-eventstream", code: 200)"""
        else -> ""
    }
    val replaced = input.replace("{protocol-name}", protocolName)
        .replace("{op-traits}", opTraits)
    output.parentFile.mkdirs()
    output.writeText(replaced)
}

val testServiceShapeId = "aws.sdk.kotlin.test.eventstream#TestService"
codegen {
    tests.forEach { test ->

        projections.register(test.projectionName) {
            imports = listOf(test.model.relativeTo(project.buildDir).toString())
            transforms = listOf(
                """
                {
                  "name": "includeServices",
                  "args": {
                    "services": ["$testServiceShapeId"]
                  }
                }
                """
            )

            smithyKotlinPlugin {
                serviceShapeId = testServiceShapeId
                packageName = "aws.sdk.kotlin.test.eventstream.${test.protocolName.toLowerCase()}"
                packageVersion = "1.0"
                buildSettings {
                    generateFullProject = false
                    generateDefaultBuildFiles = false
                    optInAnnotations = listOf(
                        "aws.smithy.kotlin.runtime.util.InternalApi",
                        "aws.sdk.kotlin.runtime.InternalSdkApi"
                    )
                }
            }
        }
    }
}

tasks.named("generateSmithyBuildConfig") {
    doFirst {
        tests.forEach { test -> fillInModel(test.model, test.protocolName) }
    }
}

val generateProjectionsTask = tasks.named<SmithyBuild>("generateSmithyProjections") {
    addCompileClasspath = true

    // ensure the generated tests use the same version of the runtime as the aws aws-runtime
    val smithyKotlinVersion: String by project
    doFirst {
        System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinVersion)
    }
}


val optinAnnotations = listOf(
    "kotlin.RequiresOptIn",
    "aws.smithy.kotlin.runtime.util.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
)
kotlin.sourceSets.all {
    optinAnnotations.forEach { languageSettings.optIn(it) }
}

kotlin.sourceSets.getByName("test") {
    codegen.projections.forEach {
        val projectedSrcDir = it.projectionRootDir.resolve("src/main/kotlin")
        kotlin.srcDir(projectedSrcDir)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>{
    dependsOn(generateProjectionsTask)
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
    val coroutinesVersion: String by project
    val smithyKotlinVersion: String by project

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("aws.smithy.kotlin:smithy-test:$smithyKotlinVersion")
    testImplementation("aws.smithy.kotlin:aws-signing-default:$smithyKotlinVersion")
    testImplementation("aws.smithy.kotlin:tracing-core:$smithyKotlinVersion")

    // have to manually add all the dependencies of the generated client(s)
    // doing it this way (as opposed to doing what we do for protocol-tests) allows
    // the tests to work without a publish to maven-local step at the cost of maintaining
    // this set of dependencies manually
    // <-- BEGIN GENERATED DEPENDENCY LIST -->
    implementation("aws.smithy.kotlin:aws-credentials:$smithyKotlinVersion")
    implementation(project(":aws-runtime:protocols:aws-event-stream"))
    implementation(project(":aws-runtime:aws-http"))
    implementation(project(":aws-runtime:protocols:aws-json-protocols"))
    implementation("aws.smithy.kotlin:aws-signing-common:$smithyKotlinVersion")
    implementation("aws.smithy.kotlin:http:$smithyKotlinVersion")
    implementation("aws.smithy.kotlin:http-client-engine-default:$smithyKotlinVersion")
    implementation("aws.smithy.kotlin:io:$smithyKotlinVersion")
    implementation("aws.smithy.kotlin:serde:$smithyKotlinVersion")
    implementation("aws.smithy.kotlin:serde-json:$smithyKotlinVersion")
    implementation("aws.smithy.kotlin:utils:$smithyKotlinVersion")
    api(project(":aws-runtime:aws-config"))
    api(project(":aws-runtime:aws-core"))
    api(project(":aws-runtime:aws-endpoint"))
    api("aws.smithy.kotlin:runtime-core:$smithyKotlinVersion")
    // <-- END GENERATED DEPENDENCY LIST -->
}