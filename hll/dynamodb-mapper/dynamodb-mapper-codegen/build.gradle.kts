/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "DynamoDbMapper code generation"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Codegen"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.codegen"

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":hll:hll-codegen"))
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
    implementation(project(":services:dynamodb"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}

/**
 * FIXME How to get this published to Maven Local?
 *
 *  ./gradlew --info --rerun-tasks :hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal
 *
 * All projects evaluated.
 * Task path ':hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal' matched project ':hll:dynamodb-mapper:dynamodb-mapper-codegen'
 * Task name matched 'publishToMavenLocal'
 * Selected primary task 'publishToMavenLocal' from project :hll:dynamodb-mapper:dynamodb-mapper-codegen
 * Tasks to be executed: [task ':hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal']
 * Tasks that were excluded: []
 * Resolve mutations for :hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal (Thread[Execution worker Thread 3,5,main]) started.
 * :hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal (Thread[Execution worker Thread 3,5,main]) started.
 *
 * > Task :hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal UP-TO-DATE
 * Skipping task ':hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal' as it has no actions.
 */
