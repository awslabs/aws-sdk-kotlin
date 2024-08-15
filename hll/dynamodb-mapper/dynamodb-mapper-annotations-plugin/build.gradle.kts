/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    `kotlin-dsl`
    alias(libs.plugins.plugin.publish)
    id("com.google.devtools.ksp") version "2.0.10-1.0.24"
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-codegen"))
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    plugins {
        create("dynamodb-mapper-annotations-plugin") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.schemagenerator"
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPlugin"
            description = "Plugin used to generate DynamoDbMapper schemas from user classes"
        }
    }
}

ksp {
    excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
}

