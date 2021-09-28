/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Support for AWS configuration"
extra["moduleName"] = "aws.sdk.kotlin.runtime.config"

val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:http:$smithyKotlinVersion")
                implementation(project(":aws-runtime:http-client-engine-crt"))
                implementation(project(":aws-runtime:protocols:http"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
            }
        }
    }
}
