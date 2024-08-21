/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Common code-generation utilities used by AWS SDK for Kotlin's high level libraries"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: Codegen"
extra["moduleName"] = "aws.sdk.kotlin.hll.codegen"

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.smithy.kotlin.runtime.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}

val sdkVersion: String by project
group = "aws.sdk.kotlin"
version = sdkVersion

publishing {
    publications {
        create<MavenPublication>("hll-codegen") {
            from(components["java"])
        }
    }
}