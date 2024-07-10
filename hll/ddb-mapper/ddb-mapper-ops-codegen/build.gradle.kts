/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                implementation(libs.ksp.api)
                implementation(project(":services:dynamodb"))
            }
        }
    }
}
