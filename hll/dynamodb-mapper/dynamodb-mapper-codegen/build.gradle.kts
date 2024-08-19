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
    api(libs.ksp.api)
    api(project(":hll:hll-codegen"))
    api(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
    api(project(":services:dynamodb"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}
