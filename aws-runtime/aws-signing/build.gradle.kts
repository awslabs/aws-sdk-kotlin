/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS Signing Support"
extra["displayName"] = "AWS :: SDK :: Kotlin :: Signing"
extra["moduleName"] = "aws.sdk.kotlin.runtime.auth.signing"

val smithyKotlinVersion: String by project
val kotestVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                val crtKotlinVersion: String by project
                // signing config uses CredentialsProvider/Credentials
                api(project(":aws-runtime:aws-types"))
                // presigner config exposes endpoint resolver
                api(project(":aws-runtime:aws-endpoint"))
                // sign() API takes HttpRequest
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

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
