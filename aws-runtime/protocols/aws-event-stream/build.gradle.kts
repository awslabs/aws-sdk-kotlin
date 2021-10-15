/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Support for the vnd.amazon.event-stream content type"
extra["displayName"] = "AWS :: SDK :: Kotlin :: Protocols :: Event Stream"
extra["moduleName"] = "aws.sdk.kotlin.runtime.protocol.eventstream"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
            }
        }

        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
