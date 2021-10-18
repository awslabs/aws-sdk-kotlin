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

        val EndpointResolverProp: ClientConfigProperty = ClientConfigProperty {
            name = "endpointResolver"
            documentation = """
                Determines the endpoint (hostname) to make requests to. When not provided a default
                resolver is configured automatically. This is an advanced client option.
            """.trimIndent()

            symbol = buildSymbol {
                name = "EndpointResolver"
                namespace(AwsKotlinDependency.AWS_CORE, subpackage = "endpoint")
            }
        }

        init {
            val awsClientConfigSymbol = buildSymbol {
                name = "AwsClientConfig"
                namespace(AwsKotlinDependency.AWS_TYPES)
            }

            RegionProp = ClientConfigProperty.String(
                "region",
                documentation = """
                    AWS region to make requests to
                """.trimIndent(),
                baseClass = awsClientConfigSymbol
            )

            CredentialsProviderProp = ClientConfigProperty {
                symbol = AwsRuntimeTypes.Types.CredentialsProvider
                baseClass = awsClientConfigSymbol
                documentation = """
                    The AWS credentials provider to use for authenticating requests. If not provided a
                    [${symbol?.namespace}.DefaultChainCredentialsProvider] instance will be used.
                """.trimIndent()
            }
        }
    }

    // FIXME - credentials and endpoint resolver need defaulted but ONLY in the constructor not the builder. We have no way (yet) of
    // expressing this
    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> =
        listOf(RegionProp, CredentialsProviderProp, EndpointResolverProp)
}
