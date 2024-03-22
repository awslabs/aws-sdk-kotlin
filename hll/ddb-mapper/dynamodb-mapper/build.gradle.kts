/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":services:dynamodb"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
