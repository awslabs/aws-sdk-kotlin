/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    kotlin("jvm")
    id("org.graalvm.buildtools.native") version "0.10.2"
    application
}

application {
    mainClass.set("aws.sdk.kotlin.example.MainKt")
}

val awsSdkKotlinVersion: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("aws.sdk.kotlin:cloudwatchlogs:$awsSdkKotlinVersion")
}

graalvmNative {
    binaries.all {
        resources.autodetect()
        // Add the META-INF/native-image directory to the classpath for reflection configuration
        configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))

    }
}

tasks.named<org.graalvm.buildtools.gradle.tasks.NativeRunTask>("nativeRun") {
    this.runtimeArgs = listOf("my-region", "my-log-group", "my-log-stream")
}