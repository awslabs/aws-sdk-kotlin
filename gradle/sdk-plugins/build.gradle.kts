/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.smithy.gradle.plugin)
    implementation(libs.smithy.model)
    implementation(libs.smithy.aws.traits)
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        val awsCodegenPlugin by creating {
            id = "aws.sdk.kotlin.codegen"
            implementationClass = "aws.sdk.kotlin.gradle.codegen.CodegenPlugin"
        }
    }
}
