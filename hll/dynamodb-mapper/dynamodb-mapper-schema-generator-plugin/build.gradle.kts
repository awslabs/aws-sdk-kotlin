/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
description = "Plugin used to generate DynamoDbMapper schemas from user classes"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Schema Generator Plugin"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.plugins"

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.ksp)
}

ksp {
    excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
}

dependencies {
//    api(project(":hll:dynamodb-mapper:dynamodb-mapper-codegen"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    plugins {
        create("dynamodb-mapper-schema-generator") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.plugins"
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPlugin"
            description = "Plugin used to generate DynamoDbMapper schemas from user classes"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
