/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "HTTP core for AWS service clients"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: HTTP"
extra["moduleName"] = "software.aws.kotlinsdk.http"

val smithyKotlinClientRtVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":client-runtime:aws-client-rt"))

                api("software.aws.smithy.kotlin:http:$smithyKotlinClientRtVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(project(":client-runtime:testing"))
            }
        }
    }
}
