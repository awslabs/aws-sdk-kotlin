/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS Service Authentication"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: Auth"
extra["moduleName"] = "aws.sdk.kotlin.runtime.auth"

val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                val crtKotlinVersion: String by project
                api(project(":client-runtime:aws-client-rt"))
                api("software.aws.smithy.kotlin:http:$smithyKotlinVersion")
                implementation(project(":client-runtime:crt-util"))
                implementation("aws.sdk.kotlin.crt:aws-crt-kotlin:$crtKotlinVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(project(":client-runtime:testing"))
            }
        }
    }
}
