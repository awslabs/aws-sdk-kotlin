/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Annotation processor for the DynamoDbMapper, used to code-generate schemas for user classes"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Annotation Processor"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.processor"

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":hll:codegen"))
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
                implementation(libs.ksp.api)
            }
        }
    }
}
