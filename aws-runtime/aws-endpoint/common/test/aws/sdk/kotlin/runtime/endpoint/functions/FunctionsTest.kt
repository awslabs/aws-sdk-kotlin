/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.endpoint.functions

import kotlin.test.*

class FunctionsTest {
    @Test
    fun testPartitionAwsCn() =
        assertEquals(AwsCnPartition, partition("cn-1"))

    @Test
    fun testPartitionAwsUsGov() =
        assertEquals(AwsUsGovPartition, partition("us-gov-1"))

    @Test
    fun testPartitionAwsIsoGov() =
        assertEquals(AwsIsoPartition, partition("us-iso-1"))

    @Test
    fun testPartitionAwsIsoBGov() =
        assertEquals(AwsIsoPartition, partition("us-iso-b-1"))

    @Test
    fun testPartitionAws() =
        assertEquals(AwsPartition, partition("us-east-1"))

    @Test
    fun testPartitionAwsFallback() =
        assertEquals(AwsPartition, partition("unknown"))

    @Test
    fun testParseInvalidArn() =
        assertNull(parseArn("invalid"))

    @Test
    fun testParseInvalidLengthArn() =
        assertNull(parseArn("arn:aws"))

    @Test
    fun testParseArn() =
        assertEquals(
            Arn(
                "partition",
                "service",
                "region",
                "account-id",
                listOf("resource-type", "resource-id"),
            ),
            parseArn("arn:partition:service:region:account-id:resource-type/resource-id")
        )
}
