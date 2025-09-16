/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    kotlin("jvm")
}

kotlin {
    sourceSets {
        main {
            dependencies {
                implementation(libs.aws.kotlin.repo.tools.ktlint.rules)
            }
        }
    }
}
