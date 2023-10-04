/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "AWS client runtime core"
extra["displayName"] = "AWS :: SDK :: Kotlin :: Client Runtime"
extra["moduleName"] = "aws.sdk.kotlin.runtime"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.smithy.kotlin.runtime.core)
                api(libs.smithy.kotlin.smithy.client)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
        }
    }
}
