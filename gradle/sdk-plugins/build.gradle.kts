/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import java.util.Properties
buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

fun loadVersions() {
    val gradleProperties = Properties()
    val propertiesFile: File = file("../../gradle.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { gradleProperties.load(it) }
    }
    gradleProperties.forEach {
        project.ext.set(it.key.toString(), it.value)
    }
}
// use the versions from aws-sdk-kotlin/gradle.properties
loadVersions()

val smithyVersion: String by project
val smithyGradleVersion: String by project

dependencies {
    implementation("software.amazon.smithy:smithy-gradle-plugin:$smithyGradleVersion")
    implementation("software.amazon.smithy:smithy-model:$smithyVersion")
    implementation("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
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

