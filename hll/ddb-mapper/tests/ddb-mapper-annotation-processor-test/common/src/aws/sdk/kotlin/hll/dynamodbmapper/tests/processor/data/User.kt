/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.tests.processor.data

import aws.sdk.kotlin.hll.dynamodbmapper.DdbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DdbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DdbPartitionKey

@DdbItem
public data class User(
    @DdbPartitionKey val id: Int,
    @DdbAttribute("fName") val givenName: String,
    @DdbAttribute("lName") val surname: String,
    val age: Int,
)
