/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("dynamodb-mapper-annotations-plugin") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.plugins"
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.AnnotationsPlugin"
        }
    }
}

