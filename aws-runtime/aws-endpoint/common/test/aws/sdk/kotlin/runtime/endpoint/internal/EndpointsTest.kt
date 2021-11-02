/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.endpoint.internal

import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.runtime.endpoint.CredentialScope
import kotlin.test.Test
import kotlin.test.assertEquals

val testPartitions = listOf(
    // normal partition with and without overrides
    Partition(
        id = "part-id-1",
        regionRegex = Regex("^(us)-\\w+-\\d+$"),
        defaults = EndpointDefinition(
            hostname = "service.{region}.amazonaws.com",
            protocols = listOf("https"),
            signatureVersions = listOf("v4")
        ),
        partitionEndpoint = "",
        isRegionalized = true,
        endpoints = mapOf(
            // region with all defaults
            "us-west-1" to EndpointDefinition(),
            // region with overrides
            "us-west-1-alt" to EndpointDefinition(
                hostname = "service-alt.us-west-1.amazonaws.com",
                protocols = listOf("http"),
                signatureVersions = listOf("vFoo"),
                credentialScope = CredentialScope("us-west-1", "foo")
            )
        )
    ),
    // global (non-regionalized) partition
    Partition(
        id = "part-id-2",
        regionRegex = Regex("^(cn)-\\w+-\\d+$"),
        defaults = EndpointDefinition(
            protocols = listOf("https"),
            signatureVersions = listOf("v4"),
            credentialScope = CredentialScope(service = "foo")
        ),
        partitionEndpoint = "partition",
        isRegionalized = false,
        endpoints = mapOf(
            "partition" to EndpointDefinition(
                hostname = "some-global-thing.amazonaws.cn",
                credentialScope = CredentialScope(region = "cn-east-1")
            ),
            "fips-partition" to EndpointDefinition(
                hostname = "some-global-thing-fips.amazonaws.cn",
                credentialScope = CredentialScope(region = "cn-east-1")
            )
        )
    ),
    // partition with only defaults
    Partition(
        id = "part-id-3",
        regionRegex = Regex("^(eu)-\\w+-\\d+$"),
        defaults = EndpointDefinition(
            hostname = "service.{region}.amazonaws.com",
            protocols = listOf("https"),
            signatureVersions = listOf("v4"),
            credentialScope = CredentialScope(service = "foo")
        ),
        partitionEndpoint = "",
        isRegionalized = true,
        endpoints = mapOf()
    ),
)

class EndpointsTest {
    private data class ResolveTest(val description: String, val region: String, val expected: AwsEndpoint)

    private val endpointResolveTestCases = listOf(
        ResolveTest(
            description = "modeled region with no endpoint overrides",
            region = "us-west-1",
            AwsEndpoint(
                "https://service.us-west-1.amazonaws.com",
                CredentialScope(region = "us-west-1")
            )
        ),
        ResolveTest(
            description = "modeled region with endpoint overrides",
            region = "us-west-1-alt",
            AwsEndpoint(
                "http://service-alt.us-west-1.amazonaws.com",
                CredentialScope(region = "us-west-1", service = "foo")
            )
        ),
        ResolveTest(
            description = "partition endpoint",
            region = "cn-central-1",
            AwsEndpoint(
                "https://some-global-thing.amazonaws.cn",
                CredentialScope(region = "cn-east-1", service = "foo")
            )
        ),
        ResolveTest(
            description = "region with un-modeled endpoints (resolved through regex)",
            region = "eu-west-1",
            AwsEndpoint(
                "https://service.eu-west-1.amazonaws.com",
                CredentialScope(region = "eu-west-1", service = "foo")
            )
        ),
        ResolveTest(
            description = "specified partition endpoint",
            region = "partition",
            AwsEndpoint(
                "https://some-global-thing.amazonaws.cn",
                CredentialScope(region = "cn-east-1", service = "foo")
            )
        ),
        ResolveTest(
            description = "fips partition endpoint",
            region = "fips-partition",
            AwsEndpoint(
                "https://some-global-thing-fips.amazonaws.cn",
                CredentialScope(region = "cn-east-1", service = "foo")
            )
        ),
    )

    @Test
    fun testResolveEndpoint() {
        endpointResolveTestCases.forEachIndexed { idx, test ->
            val actual = resolveEndpoint(testPartitions, test.region)
            assertEquals(test.expected, actual, "endpoint failed for case[$idx]: ${test.description}")
        }
    }
}
