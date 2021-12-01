/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Support for AWS configuration"
extra["moduleName"] = "aws.sdk.kotlin.runtime.config"

val smithyKotlinVersion: String by project
val crtKotlinVersion: String by project

val kotestVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api(project(":aws-runtime:aws-types"))
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:http:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:utils:$smithyKotlinVersion")
                implementation(project(":aws-runtime:http-client-engine-crt"))
                implementation(project(":aws-runtime:aws-http"))

                // parsing common JSON credentials responses
                implementation("aws.smithy.kotlin:serde-json:$smithyKotlinVersion")


                // credential providers
                implementation("aws.sdk.kotlin.crt:aws-crt-kotlin:$crtKotlinVersion")
                implementation(project(":aws-runtime:crt-util"))

            }
        }
        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
                implementation("aws.smithy.kotlin:http-test:$smithyKotlinVersion")
                val kotlinxSerializationVersion: String by project
                val mockkVersion: String by project
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("io.mockk:mockk:$mockkVersion")
            }
        }
        jvmTest {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
