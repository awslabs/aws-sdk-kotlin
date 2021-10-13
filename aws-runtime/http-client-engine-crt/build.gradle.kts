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

description = "HTTP client engine backed by CRT"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HTTP"
extra["moduleName"] = "aws.sdk.kotlin.runtime.http.engine.crt"

val smithyKotlinVersion: String by project
val coroutinesVersion: String by project
val atomicFuVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api("aws.smithy.kotlin:http:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
                implementation(project(":aws-runtime:crt-util"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

                implementation("org.jetbrains.kotlinx:atomicfu:$atomicFuVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
                implementation("aws.smithy.kotlin:http-test:$smithyKotlinVersion")
            }
        }
    }
}

