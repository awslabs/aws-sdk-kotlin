/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty

class AwsServiceConfigIntegration : KotlinIntegration {
    companion object {
        // RegionConfig properties
        val RegionProp: ClientConfigProperty

        // AuthConfig properties
        val CredentialsProviderProp: ClientConfigProperty
        val SigningRegionProp: ClientConfigProperty

        val EndpointResolverProp: ClientConfigProperty = ClientConfigProperty {
            name = "endpointResolver"
            documentation = """
                Determines the endpoint (hostname) to make requests to. When not provided a default
                resolver is configured automatically. This is an advanced client option.
            """.trimIndent()

            symbol = buildSymbol {
                name = "EndpointResolver"
                namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE, subpackage = "endpoint")
            }
        }

        init {
            val regionConfigSymbol = buildSymbol {
                name = "RegionConfig"
                namespace(AwsKotlinDependency.AWS_CLIENT_RT_REGIONS)
            }

            RegionProp = ClientConfigProperty.String(
                "region",
                documentation = """
                    AWS region to make requests to
                """.trimIndent(),
                baseClass = regionConfigSymbol
            )

            val authConfigSymbol = buildSymbol {
                name = "AuthConfig"
                namespace(AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
            }

            SigningRegionProp = ClientConfigProperty.String(
                "signingRegion",
                documentation = """
                    AWS region to be used for signing the request. This is not necessarily the same as `region`
                    in the case of global services like IAM
                """.trimIndent(),
                baseClass = authConfigSymbol
            )

            CredentialsProviderProp = ClientConfigProperty {
                symbol = buildSymbol {
                    name = "CredentialsProvider"
                    namespace(AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
                }
                baseClass = authConfigSymbol
                documentation = """
                    The AWS credentials provider to use for authenticating requests. If not provided a
                    [${symbol?.namespace}.DefaultChainCredentialsProvider] instance will be used.
                """.trimIndent()
            }
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> {
        return listOf(RegionProp, SigningRegionProp, CredentialsProviderProp, EndpointResolverProp)
    }
}
