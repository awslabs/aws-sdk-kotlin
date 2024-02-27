/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.tests.processor.data

import aws.sdk.kotlin.hll.dynamodbmapper.DdbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DdbPartitionKey

@DdbItem
public data class Group(
    @DdbPartitionKey val name: String,
    val userIds: String,
)
