/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(kotlin("gradle-plugin-api"))

    implementation(libs.smithy.model)
    implementation(libs.kotlin.gradle.plugin)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test.junit5)
}

gradlePlugin {
    plugins {
        create("ddbmapper-ops-codegen") {
            id = "ddbmapper-ops-codegen"
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.operations.codegen.CodegenPlugin"
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
