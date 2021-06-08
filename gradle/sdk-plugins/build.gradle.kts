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
    kotlin("jvm") version "1.5.0"
}

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

// FIXME - load from root project gradle.properties
val kotlinVersion: String = "1.5.0"
val smithyVersion: String = "1.7.2"
val smithyGradleVersion: String = "0.5.3"

dependencies {
    implementation("software.amazon.smithy:smithy-gradle-plugin:$smithyGradleVersion")
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

