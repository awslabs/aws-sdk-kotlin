/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.awsjson.aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.EndpointResolverGenerator
import io.kotest.matchers.string.shouldContainOnlyOnce
import org.junit.jupiter.api.Test
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.kotlin.codegen.test.generateTestContext
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.node.Node

class EndpointResolverGeneratorTest {

    private fun getGeneratedResolverContents(model: String): String {
        val ctx = model.toSmithyModel().generateTestContext("test", "Example")
        val endpointData = Node.parse(endpointsJson).expectObjectNode()
        EndpointResolverGenerator(endpointData).render(ctx)
        ctx.delegator.flushWriters()
        val manifest = ctx.delegator.fileManifest as MockManifest
        return manifest.expectFileString("src/main/kotlin/test/internal/DefaultEndpointResolver.kt")
    }

    @Test
    fun testRenderWithMatchingRegions() {
        val model = """
            namespace test

            use aws.protocols#awsJson1_1
            use aws.api#service

            @service(sdkId: "service with overrides", endpointPrefix: "service-with-overrides")
            @awsJson1_1
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {}
        """
        val contents = getGeneratedResolverContents(model)

        val expected = """
private val servicePartitions = listOf(
    Partition(
        id = "aws",
        regionRegex = Regex("^(us|eu|ap|sa|ca|me|af)\\-\\w+\\-\\d+${'$'}"),
        partitionEndpoint = "",
        isRegionalized = true,
        defaults = EndpointDefinition(
            hostname = "service-with-overrides.{region}.amazonaws.com",
            protocols = listOf("https", ),
            signatureVersions = listOf("v4", ),
        ),
        endpoints = mapOf(
            "eu-central-1" to EndpointDefinition(),
            "eu-west-1" to EndpointDefinition(),
            "us-west-2" to EndpointDefinition(),
            "fips-us-west-2" to EndpointDefinition(
                hostname = "service-with-overrides-fips.us-west-2.amazonaws.com",
                credentialScope = CredentialScope(region = "us-west-2",),
            ),
        )
    ),
    Partition(
        id = "aws-us-gov",
        regionRegex = Regex("^us\\-gov\\-\\w+\\-\\d+${'$'}"),
        partitionEndpoint = "",
        isRegionalized = true,
        defaults = EndpointDefinition(
            hostname = "service-with-overrides.{region}.amazonaws.com",
            protocols = listOf("https", ),
            signatureVersions = listOf("v4", ),
        ),
        endpoints = mapOf(
            "us-gov-east-1" to EndpointDefinition(
                hostname = "service-with-overrides.us-gov-east-1.amazonaws.com",
                credentialScope = CredentialScope(region = "us-gov-east-1",),
            ),
            "us-gov-west-1" to EndpointDefinition(
                hostname = "service-with-overrides.us-gov-west-1.amazonaws.com",
                credentialScope = CredentialScope(region = "us-gov-west-1",),
            ),
        )
    ),
)"""

        contents.shouldContainOnlyOnce(expected)
    }

    @Test
    fun testRenderWithNoMatches() {
        val model = """
            namespace test

            use aws.protocols#awsJson1_1
            use aws.api#service

            @service(sdkId: "service with overrides", endpointPrefix: "service-all-defaults")
            @awsJson1_1
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {}
        """

        val contents = getGeneratedResolverContents(model)

        val expected = """
private val servicePartitions = listOf(
    Partition(
        id = "aws",
        regionRegex = Regex("^(us|eu|ap|sa|ca|me|af)\\-\\w+\\-\\d+${'$'}"),
        partitionEndpoint = "",
        isRegionalized = true,
        defaults = EndpointDefinition(
            hostname = "service-all-defaults.{region}.amazonaws.com",
            protocols = listOf("https", ),
            signatureVersions = listOf("v4", ),
        ),
        endpoints = mapOf(
        )
    ),
    Partition(
        id = "aws-us-gov",
        regionRegex = Regex("^us\\-gov\\-\\w+\\-\\d+${'$'}"),
        partitionEndpoint = "",
        isRegionalized = true,
        defaults = EndpointDefinition(
            hostname = "service-all-defaults.{region}.amazonaws.com",
            protocols = listOf("https", ),
            signatureVersions = listOf("v4", ),
        ),
        endpoints = mapOf(
        )
    ),
)"""

        contents.shouldContainOnlyOnce(expected)
    }

    @Test
    fun testRenderResolverImplementation() {
        val model = """
            namespace test

            use aws.protocols#awsJson1_1
            use aws.api#service

            @service(sdkId: "service with overrides", endpointPrefix: "service-all-defaults")
            @awsJson1_1
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {}
        """
        val contents = getGeneratedResolverContents(model)

        val expected = """
internal class DefaultEndpointResolver : EndpointResolver {
    override suspend fun resolve(service: String, region: String): Endpoint {
        return resolveEndpoint(servicePartitions, region) ?: throw ClientException("unable to resolve endpoint for region: ${'$'}region")
    }
}"""
        contents.shouldContainOnlyOnce(expected)
    }
}

private const val endpointsJson = """
{
  "partitions": [
    {
      "defaults": {
        "hostname": "{service}.{region}.{dnsSuffix}",
        "protocols": [
          "https"
        ],
        "signatureVersions": [
          "v4"
        ]
      },
      "dnsSuffix": "amazonaws.com",
      "partition": "aws",
      "partitionName": "AWS Standard",
      "regionRegex": "^(us|eu|ap|sa|ca|me|af)\\-\\w+\\-\\d+${'$'}",
      "regions": {
        "eu-central-1": {
          "description": "Europe (Frankfurt)"
        },
        "eu-west-1": {
          "description": "Europe (Ireland)"
        },
        "us-west-2": {
          "description": "US West (Oregon)"
        },
        "us-west-2-fips": {
          "description": "US West (Oregon) FIPS"
        }
      },
      "services": {
        "service-with-overrides": {
          "endpoints": {
            "eu-central-1": {},
            "eu-west-1": {},
            "us-west-2": {},
            "fips-us-west-2": {
              "credentialScope": {
                "region": "us-west-2"
              },
              "hostname": "service-with-overrides-fips.us-west-2.amazonaws.com"
            }
          }
        },
        "service-all-defaults": {
          "endpoints": {
          }
        }
      }
    },
    {
      "defaults": {
        "hostname": "{service}.{region}.{dnsSuffix}",
        "protocols": [
          "https"
        ],
        "signatureVersions": [
          "v4"
        ]
      },
      "dnsSuffix": "amazonaws.com",
      "partition": "aws-us-gov",
      "partitionName": "AWS GovCloud (US)",
      "regionRegex": "^us\\-gov\\-\\w+\\-\\d+${'$'}",
      "regions": {
        "us-gov-east-1": {
          "description": "AWS GovCloud (US-East)"
        },
        "us-gov-west-1": {
          "description": "AWS GovCloud (US-West)"
        }
      },
      "services": {
        "service-with-overrides" : {
          "endpoints" : {
            "us-gov-east-1" : {
              "credentialScope" : {
                "region" : "us-gov-east-1"
              },
              "hostname" : "service-with-overrides.us-gov-east-1.amazonaws.com"
            },
            "us-gov-west-1" : {
              "credentialScope" : {
                "region" : "us-gov-west-1"
              },
              "hostname" : "service-with-overrides.us-gov-west-1.amazonaws.com"
            }
          }
        }
      }
    }
  ],
  "version": 3
}
"""
