/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.*

description = "AWS client runtime support for generated service clients"

plugins {
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.12.1"
    jacoco
}

val sdkVersion: String by project

val coroutinesVersion: String by project
val kotestVersion: String by project
val slf4jVersion: String by project

subprojects {
    if (!needsKmpConfigured) return@subprojects

    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin("org.jetbrains.dokka")
    }

    configurePublishing("aws-sdk-kotlin")

    kotlin {
        explicitApi()

        sourceSets {
            // dependencies available for all subprojects
            named("commonMain") {
                dependencies {
                    // FIXME - refactor to only projects that need this
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                }
            }

            named("commonTest") {
                dependencies {
                    implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                }
            }

            named("jvmTest") {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
                    implementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
                    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                }
            }
        }
    }

    kotlin.sourceSets.all {
        // Allow subprojects to use internal APIs
        // See https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
        listOf("kotlin.RequiresOptIn").forEach { languageSettings.optIn(it) }
    }

    dependencies {
        dokkaPlugin(project(":dokka-aws"))
    }
}

apiValidation {
    nonPublicMarkers.add("aws.sdk.kotlin.runtime.InternalSdkApi")
}
