/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Support for the JSON suite of AWS protocols"
extra["displayName"] = "AWS :: SDK :: Kotlin :: JSON"
extra["moduleName"] = "aws.sdk.kotlin.runtime.protocol.json"

val coroutinesVersion: String by project
val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api("aws.smithy.kotlin:http:$smithyKotlinVersion")
                api(project(":aws-runtime:aws-core"))
                implementation(project(":aws-runtime:aws-http"))
                implementation("aws.smithy.kotlin:serde:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:serde-json:$smithyKotlinVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
