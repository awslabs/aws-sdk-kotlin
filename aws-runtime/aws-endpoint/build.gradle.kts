/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS Endpoint Support"
extra["displayName"] = "AWS :: SDK :: Kotlin :: Endpoint"
extra["moduleName"] = "aws.sdk.kotlin.runtime.endpoint"

val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                implementation(project(":aws-runtime:aws-core"))
                // exposes Endpoint
                api("aws.smithy.kotlin:http:$smithyKotlinVersion")
            }
        }

        all {
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
