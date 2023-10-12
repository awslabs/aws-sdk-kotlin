/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    kotlin("jvm")
}

description = "Custom Dokka plugin for AWS Kotlin SDK API docs"

dependencies {
    compileOnly(libs.dokka.base)
    compileOnly(libs.dokka.core)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = false // FIXME Dokka bundles stdlib into the classpath, causing an unfixable warning
    }
}
