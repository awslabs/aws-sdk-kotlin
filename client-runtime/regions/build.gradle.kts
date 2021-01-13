/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS Region Support"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: Regions"
extra["moduleName"] = "software.aws.kotlinsdk.regions"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":client-runtime:aws-client-rt"))
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