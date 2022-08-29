/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
// This package implements AWS-specific standard library functions used by endpoint resolvers of AWS SDKs.
package aws.sdk.kotlin.runtime.endpoint.functions

import aws.sdk.kotlin.runtime.InternalSdkApi

// the number of top-level components an arn contains (separated by colons)
private const val ARN_COMPONENT_COUNT = 6

@InternalSdkApi
public fun partition(region: String): Partition =
    when {
        region.startsWith("cn-") -> AwsCnPartition
        region.startsWith("us-gov-") -> AwsUsGovPartition
        region.startsWith("us-iso-") -> AwsIsoPartition
        region.startsWith("us-iso-b-") -> AwsIsoBPartition
        else -> AwsPartition
    }

@InternalSdkApi
public fun parseArn(value: String): Arn? {
    val split = value.split(':', limit = ARN_COMPONENT_COUNT)
    if (split[0] != "arn") return null
    if (split.size != ARN_COMPONENT_COUNT) return null

    return Arn(
        split[1],
        split[2],
        split[3],
        split[4],
        split[5].split(':', '/'),
    )
}

@InternalSdkApi
public data class Partition(
    public val name: String,
    public val dnsSuffix: String,
    public val dnsDualStackSuffix: String,
    public val supportsFips: Boolean,
    public val supportsDualStack: Boolean,
)

@InternalSdkApi
public val AwsPartition: Partition =
    Partition(
        name = "aws",
        dnsSuffix = "amazonaws.com",
        dnsDualStackSuffix = "api.aws",
        supportsFips = true,
        supportsDualStack = true,
    )

@InternalSdkApi
public val AwsCnPartition: Partition =
    Partition(
        name = "aws-cn",
        dnsSuffix = "amazonaws.com.cn",
        dnsDualStackSuffix = "api.amazonwebservices.com.cn",
        supportsFips = true,
        supportsDualStack = true,
    )

@InternalSdkApi
public val AwsIsoPartition: Partition =
    Partition(
        name = "aws-iso",
        dnsSuffix = "c2s.ic.gov",
        dnsDualStackSuffix = "c2s.ic.gov",
        supportsFips = true,
        supportsDualStack = false,
    )

@InternalSdkApi
public val AwsIsoBPartition: Partition =
    Partition(
        name = "aws-iso-b",
        dnsSuffix = "sc2s.sgov.gov",
        dnsDualStackSuffix = "sc2s.sgov.gov",
        supportsFips = true,
        supportsDualStack = false,
    )

@InternalSdkApi
public val AwsUsGovPartition: Partition =
    Partition(
        name = "aws-us-gov",
        dnsSuffix = "amazonaws.com",
        dnsDualStackSuffix = "api.aws",
        supportsFips = true,
        supportsDualStack = true,
    )

@InternalSdkApi
public data class Arn(
    public val partition: String,
    public val service: String,
    public val region: String,
    public val accountId: String,
    public val resourceId: List<String>,
)
