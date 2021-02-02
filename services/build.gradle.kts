/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 *
 */
plugins {
    kotlin("jvm")
}

val sdkVersion: String = "0.0.1"

val experimentalAnnotations = listOf(
    "software.aws.clientrt.util.InternalAPI",
    "aws.sdk.kotlin.runtime.InternalSdkApi"
)

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }


    // have generated sdk's opt-in to internal runtime features
    kotlin.sourceSets.all {
        experimentalAnnotations.forEach { languageSettings.useExperimentalAnnotation(it) }
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
}