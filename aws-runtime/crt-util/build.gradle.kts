/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

buildscript {
    val atomicFuVersion: String by project
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicFuVersion")
    }
}

apply(plugin = "kotlinx-atomicfu")

description = "Utilities for working with AWS CRT Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: CRT :: Util"
extra["moduleName"] = "aws.sdk.kotlin.runtime.crt"

val atomicFuVersion: String by project
val coroutinesVersion: String by project
val crtKotlinVersion: String by project
val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api("aws.sdk.kotlin.crt:aws-crt-kotlin:$crtKotlinVersion")
                api("aws.smithy.kotlin:http:$smithyKotlinVersion")
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicFuVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}
