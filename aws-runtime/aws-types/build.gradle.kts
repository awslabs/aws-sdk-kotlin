/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Common AWS Types"
extra["displayName"] = "AWS :: SDK :: Kotlin :: Types"
extra["moduleName"] = "aws.sdk.kotlin.runtime"
val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // exposes Instant
                api("aws.smithy.kotlin:runtime-core:$smithyKotlinVersion")

                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
        }
    }
}
