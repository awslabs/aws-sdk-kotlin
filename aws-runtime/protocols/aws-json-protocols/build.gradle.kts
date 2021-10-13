/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Support for the JSON suite of AWS protocols"
extra["displayName"] = "AWS :: SDK :: Kotlin :: JSON"
extra["moduleName"] = "aws.sdk.kotlin.runtime.protocol.json"

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
                implementation("aws.smithy.kotlin:utils:$smithyKotlinVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
            }
        }
    }
}

