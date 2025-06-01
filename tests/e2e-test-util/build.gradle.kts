/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

description = "Test utilities for integration and e2e tests"

dependencies {
    api(libs.smithy.kotlin.http.client.engine.default)
    api(libs.smithy.kotlin.http.client.engine.crt)
}
