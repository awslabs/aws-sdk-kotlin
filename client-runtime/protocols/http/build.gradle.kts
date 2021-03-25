/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "HTTP core for AWS service clients"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: HTTP"
extra["moduleName"] = "aws.sdk.kotlin.runtime.http"

val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":client-runtime:aws-client-rt"))
                api(project(":client-runtime:regions"))
                api("software.aws.smithy.kotlin:http:$smithyKotlinVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(project(":client-runtime:testing"))
            }
        }
    }
}

