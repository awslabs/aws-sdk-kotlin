/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
buildscript {
    repositories {
        mavenCentral()
    }

    val atomicFuVersion: String by project

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicFuVersion")
    }
}

plugins {
    kotlin("multiplatform")
    application
}

application {
    mainClass.set("aws.sdk.kotlin.benchmarks.service.BenchmarkHarnessKt")
}

extra.set("skipPublish", true)

val platforms = listOf("common", "jvm")

platforms.forEach { platform ->
    apply(from = rootProject.file("gradle/$platform.gradle"))
}

val requiredServices = setOf(
    // Top 7 services called by Kotlin SDK customers as of 7/25/2023, in descending order of call volume
    "s3",
    "sns",
    "sts",
    "cloudwatch",
    "cloudwatchevents",
    "dynamodb",
    "pinpoint",

    // Services required as prerequisites for setup
    "iam", // Create roles for STS::AssumeRole
)

val missingServices = requiredServices.filterNot { rootProject.file("services/$it/build.gradle.kts").exists() }

if (missingServices.isEmpty()) {
    val optinAnnotations = listOf("kotlin.RequiresOptIn", "aws.smithy.kotlin.runtime.InternalApi")

    kotlin {
        sourceSets {
            all {
                val srcDir = if (name.endsWith("Main")) "src" else "test"
                val resourcesPrefix = if (name.endsWith("Test")) "test-" else ""
                // the name is always the platform followed by a suffix of either "Main" or "Test" (e.g. jvmMain, commonTest, etc)
                val platform = name.substring(0, name.length - 4)
                kotlin.srcDir("$platform/$srcDir")
                resources.srcDir("$platform/${resourcesPrefix}resources")
                languageSettings.progressiveMode = true
                optinAnnotations.forEach { languageSettings.optIn(it) }
            }

            val atomicFuVersion: String by project
            val coroutinesVersion: String by project
            val smithyKotlinVersion: String by project

            commonMain {
                dependencies {
                    api("aws.smithy.kotlin:runtime-core:$smithyKotlinVersion")
                    implementation(project(":aws-runtime:aws-core"))
                    implementation("org.jetbrains.kotlinx:atomicfu:$atomicFuVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

                    requiredServices.forEach { implementation(project(":services:$it")) }
                }
            }
        }
    }
} else {
    logger.warn(
        "Skipping build for {} project, missing the following services: {}. To ensure this project builds, run the " +
            "{}:bootstrapAll task.",
        project.name,
        missingServices.joinToString(", "),
        project.path,
    )
}

tasks.register("bootstrapAll") {
    val bootstrapArg = requiredServices.joinToString(",") { "+$it" }
    val bootstrapProj = project(":codegen:sdk")
    bootstrapProj.ext.set("aws.services", bootstrapArg)
    dependsOn(":codegen:sdk:bootstrap")
}

tasks.named<JavaExec>("run") {
    classpath += objects.fileCollection().from(
        tasks.named("compileKotlinJvm"),
        configurations.named("jvmRuntimeClasspath"),
    )
}
