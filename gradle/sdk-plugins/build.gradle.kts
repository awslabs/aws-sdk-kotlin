/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm") version "1.4.20"
}

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

// FIXME - load from root project gradle.properties
val kotlinVersion: String = "1.4.20"
val smithyVersion: String = "1.5.1"

dependencies {
    implementation("software.amazon.smithy:smithy-gradle-plugin:0.5.2")
    implementation("software.amazon.smithy:smithy-model:$smithyVersion")
    implementation("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
}

gradlePlugin {
    plugins {
        val awsSdkPlugin by creating {
            id = "aws.sdk.kotlin.sdk"
            implementationClass = "aws.sdk.kotlin.build.plugin.AwsSdkPlugin"
        }
    }
}

