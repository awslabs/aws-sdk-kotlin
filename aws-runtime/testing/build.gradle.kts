/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Internal test utilities"

val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api("aws.smithy.kotlin:testing:$smithyKotlinVersion")
                api("aws.smithy.kotlin:utils:$smithyKotlinVersion")
            }
        }
    }
}
