/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "aws.sdk.kotlin"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(kotlin("gradle-plugin-api"))

    // Smithy dependencies for codegen integration
    implementation(libs.smithy.model)
    implementation(libs.smithy.aws.traits)
    implementation(libs.smithy.protocol.traits)
    implementation(libs.smithy.kotlin.codegen)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test.junit5)
}

gradlePlugin {
    plugins {
        create("custom-sdk-build") {
            id = "aws.sdk.kotlin.custom-sdk-build"
            implementationClass = "aws.sdk.kotlin.gradle.customsdk.CustomSdkBuildPlugin"
        }
    }
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
