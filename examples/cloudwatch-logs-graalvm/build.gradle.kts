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
val kotlinxDatetimeVersion: String by project

kotlin {
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("aws.sdk.kotlin:cloudwatchlogs:$awsSdkKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
}

graalvmNative {
    binaries.all {
        resources.autodetect()
        // Add the META-INF/native-image directory to the classpath for reflection configuration
        configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))
    }
}

tasks.named<org.graalvm.buildtools.gradle.tasks.NativeRunTask>("nativeRun") {
    this.runtimeArgs = listOf("ap-southeast-1", "my-log-group", "my-log-stream")
}
