/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    kotlin("jvm")
}

description = "Test utilities for integration and e2e tests"

val smithyKotlinVersion: String by project

dependencies {
    api("aws.smithy.kotlin:http-client-engine-default:$smithyKotlinVersion")
    api("aws.smithy.kotlin:http-client-engine-crt:$smithyKotlinVersion")
}
