/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

description = "Test utilities for integration and e2e tests"

val smithyKotlinVersion: String by project

dependencies {
    api("aws.smithy.kotlin:http-client-engine-default:$smithyKotlinVersion")
    api("aws.smithy.kotlin:http-client-engine-crt:$smithyKotlinVersion")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}
