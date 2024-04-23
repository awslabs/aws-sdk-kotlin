/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

val sdkVersion: String by project
description = "Codegen support for DynamoDB Mapper operations"
group = "aws.sdk.kotlin"
version = sdkVersion

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    api(libs.smithy.kotlin.codegen)
    api(libs.smithy.aws.kotlin.codegen)
    api(project(":codegen:aws-sdk-codegen"))

    api(libs.smithy.aws.traits)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.smithy.kotlin.codegen.testutils)

    testImplementation(libs.slf4j.api)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.kotlinx.serialization.json)
}
