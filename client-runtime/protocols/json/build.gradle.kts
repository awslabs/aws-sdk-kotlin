/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Support for the JSON suite of AWS protocols"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: JSON"
extra["moduleName"] = "aws.sdk.kotlin.runtime.protocol.json"

val smithyKotlinClientRtVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api("software.aws.smithy.kotlin:http:$smithyKotlinClientRtVersion")
                api(project(":client-runtime:aws-client-rt"))
                implementation(project(":client-runtime:protocols:http"))
                implementation("software.aws.smithy.kotlin:serde:$smithyKotlinClientRtVersion")
                implementation("software.aws.smithy.kotlin:serde-json:$smithyKotlinClientRtVersion")
                implementation("software.aws.smithy.kotlin:utils:$smithyKotlinClientRtVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(project(":client-runtime:testing"))
            }
        }
    }
}

