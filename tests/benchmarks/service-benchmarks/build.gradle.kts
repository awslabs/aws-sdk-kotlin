/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.skipPublishing
buildscript {
    repositories {
        mavenCentral()
    }

    val atomicFuVersion: String by project

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicFuVersion")
    }
}

apply(plugin = "kotlinx-atomicfu")

plugins {
    kotlin("multiplatform")
    application
}

application {
    mainClass.set("aws.sdk.kotlin.benchmarks.service.BenchmarkHarnessKt")
}

skipPublishing()

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
                optinAnnotations.forEach { languageSettings.optIn(it) }
            }

            val atomicFuVersion: String by project
            val coroutinesVersion: String by project
            val smithyKotlinVersion: String by project

            jvmMain {
                dependencies {
                    api(libs.smithy.kotlin.runtime.core)
                    implementation(project(":aws-runtime:aws-core"))
                    implementation(libs.kotlinx.atomicfu)
                    implementation(libs.kotlinx.coroutines.core)

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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        enabled = false
    }
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
