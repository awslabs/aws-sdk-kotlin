/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Utilities for working with AWS CRT Kotlin"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: CRT :: Util"
extra["moduleName"] = "aws.sdk.kotlin.runtime.crt"

val smithyKotlinVersion: String by project
val crtKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api("aws.sdk.kotlin.crt:aws-crt-kotlin:$crtKotlinVersion")
                api("aws.smithy.kotlin:http:$smithyKotlinVersion")
            }
        }
    }
}

