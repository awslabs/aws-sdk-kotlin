/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Support for the XML suite of AWS protocols"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: XML"
extra["moduleName"] = "aws.sdk.kotlin.runtime.protocol.xml"

val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api("aws.smithy.kotlin:http:$smithyKotlinVersion")
                api(project(":aws-runtime:aws-core"))
                implementation(project(":aws-runtime:protocols:http"))
                implementation("aws.smithy.kotlin:serde:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:serde-xml:$smithyKotlinVersion")
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

