/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "AWS Endpoint Support"
extra["displayName"] = "AWS :: SDK :: Kotlin :: Endpoint"
extra["moduleName"] = "aws.sdk.kotlin.runtime.endpoint"

val smithyKotlinVersion = libs.versions.smithy.kotlin.version.get()

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":aws-runtime:aws-core"))
                // exposes Endpoint
                api("aws.smithy.kotlin:http-client:$smithyKotlinVersion")
                api("aws.smithy.kotlin:aws-signing-common:$smithyKotlinVersion")
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
