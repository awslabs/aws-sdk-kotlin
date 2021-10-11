/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS Endpoint Support"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: Auth"
extra["moduleName"] = "aws.sdk.kotlin.runtime.endpoint"

val smithyKotlinVersion: String by project
val kotestVersion: String by project

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                implementation(project(":aws-runtime:aws-core"))
            }
        }
    }
}
