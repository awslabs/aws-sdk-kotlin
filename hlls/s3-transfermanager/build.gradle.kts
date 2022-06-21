/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "S3 Transfer Manager"
extra["displayName"] = "AWS :: SDK :: Kotlin :: S3 Transfer Manager"
extra["moduleName"] = "aws.sdk.kotlin.s3.transfermanager"

val releasedSdkVersion: String by project
val smithyKotlinVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
                println("Resolved S3 client version to $releasedSdkVersion")
                implementation("aws.sdk.kotlin:s3:$releasedSdkVersion")
            }
        }
    }
}
