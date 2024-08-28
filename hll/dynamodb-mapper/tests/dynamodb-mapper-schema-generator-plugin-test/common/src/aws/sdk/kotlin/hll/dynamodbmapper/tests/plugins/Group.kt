/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.tests.plugins

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class Group(
    @DynamoDbPartitionKey val name: String,
    val userIds: String,
)
