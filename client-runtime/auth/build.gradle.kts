/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS Service Authentication"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: Auth"
extra["moduleName"] = "software.aws.kotlinsdk.auth"

val smithyKotlinClientRtVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                val crtKotlinVersion: String by project
                api(project(":client-runtime:aws-client-rt"))
                api("software.aws.smithy.kotlin:http:$smithyKotlinClientRtVersion")
                implementation(project(":client-runtime:crt-util"))
                implementation("software.amazon.awssdk.crt:aws-crt-kotlin:$crtKotlinVersion")
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