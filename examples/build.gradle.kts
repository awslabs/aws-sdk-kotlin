/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    kotlin("jvm") version "1.8.22"
}

val awsSdkKotlinVersion: String by project

allprojects {
    group = "aws.sdk.kotlin.example"
    version = awsSdkKotlinVersion

    repositories {
        mavenLocal()
        mavenCentral()
    }
}
