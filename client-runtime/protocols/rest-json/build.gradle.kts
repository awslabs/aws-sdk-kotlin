/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "JSON protocol support for AWS service clients"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: RestJSON"
extra["moduleName"] = "software.aws.kotlinsdk.restjson"

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

// Resolves build deadlock with aws-client-rt
tasks["generatePomFileForJvmPublication"]
    .dependsOn(":client-runtime:aws-client-rt:generatePomFileForJvmPublication")