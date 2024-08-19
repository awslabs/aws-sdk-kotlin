/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "DynamoDbMapper code generation"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Codegen"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.codegen"

plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.ksp.api)
                api(project(":hll:hll-codegen"))
                api(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
                api(project(":services:dynamodb"))
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.junit.jupiter)
                implementation(libs.junit.jupiter.params)
                implementation(libs.kotest.assertions.core.jvm)
                implementation(libs.kotlin.test.junit5)
            }
        }
    }
}

/**
 * Cannot. get. this. project. in. mavenLocal.
 *
 * Task path ':hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal' matched project ':hll:dynamodb-mapper:dynamodb-mapper-codegen'
 * Task name matched 'publishToMavenLocal'
 * Selected primary task 'publishToMavenLocal' from project :hll:dynamodb-mapper:dynamodb-mapper-codegen
 * Tasks to be executed: [task ':hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal']
 * Tasks that were excluded: []
 * Resolve mutations for :hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal (Thread[Execution worker Thread 7,5,main]) started.
 * :hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal (Thread[Execution worker Thread 7,5,main]) started.
 * Invalidating in-memory cache of /Users/lauzmata/aws-sdk-kotlin/.gradle/8.5/executionHistory/executionHistory.bin
 *
 * > Task :hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal UP-TO-DATE
 * Skipping task ':hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal' as it has no actions.
 */
