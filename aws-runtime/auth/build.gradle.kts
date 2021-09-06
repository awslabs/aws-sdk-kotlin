/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS Service Authentication"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: Auth"
extra["moduleName"] = "aws.sdk.kotlin.runtime.auth"

val smithyKotlinVersion: String by project
val kotestVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                val crtKotlinVersion: String by project
                api(project(":aws-runtime:aws-core"))
                api("aws.smithy.kotlin:http:$smithyKotlinVersion")
                implementation(project(":aws-runtime:crt-util"))
                implementation("aws.sdk.kotlin.crt:aws-crt-kotlin:$crtKotlinVersion")
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
            }
        }
        jvmTest {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
            }
        }
    }
}
