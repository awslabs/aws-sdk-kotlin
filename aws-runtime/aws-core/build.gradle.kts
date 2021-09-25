/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS client runtime core"
extra["displayName"] = "Software :: AWS :: Kotlin SDK :: Client Runtime"
extra["moduleName"] = "aws.sdk.kotlin.runtime"


val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api("aws.smithy.kotlin:runtime-core:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
            }
        }
        commonTest {
            dependencies {
                val kotlinxSerializationVersion: String by project
                val mockkVersion: String by project
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
                implementation("io.mockk:mockk:$mockkVersion")
                implementation(project(":aws-runtime:testing"))
            }
        }
    }
}
