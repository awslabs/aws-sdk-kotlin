/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "HTTP core for AWS service clients"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HTTP"
extra["moduleName"] = "aws.sdk.kotlin.runtime.http"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api(project(":aws-runtime:aws-endpoint"))
                api(libs.smithy.kotlin.aws.signing.common)
                api(libs.smithy.kotlin.http.client)
                api(libs.smithy.kotlin.http.auth.aws)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                api(libs.smithy.kotlin.http.test)
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
