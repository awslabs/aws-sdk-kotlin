/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Support for the XML suite of AWS protocols"
extra["displayName"] = "AWS :: SDK :: Kotlin :: XML"
extra["moduleName"] = "aws.sdk.kotlin.runtime.protocol.xml"

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
                implementation("aws.smithy.kotlin:serde-xml:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:utils:$smithyKotlinVersion")
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
                implementation(project(":aws-runtime:testing"))
            }
        }

        all {
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
