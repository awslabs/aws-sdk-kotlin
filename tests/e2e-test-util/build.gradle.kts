/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    kotlin("jvm")
}

description = "Test utilities for integration and e2e tests"

val smithyKotlinVersion = libs.versions.smithy.kotlin.version.get()

dependencies {
    api(libs.smithy.kotlin.http.client.engine.default)
    api(libs.smithy.kotlin.http.client.engine.crt)
}
