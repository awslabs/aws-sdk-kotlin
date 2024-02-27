/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.tests.processor.data

import aws.sdk.kotlin.hll.dynamodbmapper.DynamodDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class User(
    @DynamoDbPartitionKey val id: Int,
    @DynamodDbAttribute("fName") val givenName: String,
    @DynamodDbAttribute("lName") val surname: String,
    val age: Int,
)
