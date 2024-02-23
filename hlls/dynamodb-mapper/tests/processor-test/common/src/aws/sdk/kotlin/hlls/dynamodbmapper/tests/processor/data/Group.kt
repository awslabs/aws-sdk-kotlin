/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hlls.dynamodbmapper.tests.processor.data

import aws.sdk.kotlin.hlls.dynamodbmapper.DdbItem
import aws.sdk.kotlin.hlls.dynamodbmapper.DdbPartitionKey

@DdbItem
public data class Group(
    @DdbPartitionKey val name: String,
    val userIds: String,
)
