/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

val sdkVersion: String by project

plugins {
    id("aws.sdk.kotlin.hll.dynamodbmapper.schema.generator") version "1.3.17-SNAPSHOT"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper"))
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin"))
            }
        }
    }
}
