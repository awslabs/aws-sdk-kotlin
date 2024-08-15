/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
description = "Plugin used to generate DynamoDbMapper schemas from user classes"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Schema Generator Plugin"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.plugins"

plugins {
    `kotlin-dsl`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.ksp)
}

// TODO Do we want this plugin to be manually versioned?
val sdkVersion: String by project
version = sdkVersion

dependencies {
    implementation(gradleApi())
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-codegen"))
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
}

// FIXME publishToMavenLocal drops this plugin in ~/.m2/repository/aws-sdk-kotlin/... instead of
// ~/.m2/repository/aws/sdk/kotlin/...?
gradlePlugin {
    plugins {
        create("dynamodb-mapper-schema-generator") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.schema.generator"
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPlugin"
            description = "Plugin used to generate DynamoDbMapper schemas from user classes"
        }
    }
}

ksp {
    excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
}

