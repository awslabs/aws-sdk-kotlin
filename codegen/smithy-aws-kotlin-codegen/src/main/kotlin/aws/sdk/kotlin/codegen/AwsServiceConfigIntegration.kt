/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.protocols.core.EndpointResolverGenerator
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.boxed
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigPropertyType

class AwsServiceConfigIntegration : KotlinIntegration {
    companion object {
        // RegionConfig properties
        val RegionProp: ClientConfigProperty

        // AuthConfig properties
        val CredentialsProviderProp: ClientConfigProperty

        init {
            val awsClientConfigSymbol = buildSymbol {
                name = "AwsClientConfig"
                namespace(AwsKotlinDependency.AWS_TYPES, subpackage = "client")
            }

            RegionProp = ClientConfigProperty {
                name = "region"
                symbol = KotlinTypes.String.toBuilder().boxed().build()
                baseClass = awsClientConfigSymbol
                documentation = """
                    AWS region to make requests to
                """.trimIndent()
                propertyType = ClientConfigPropertyType.Required()
            }

            CredentialsProviderProp = ClientConfigProperty {
                val defaultProvider = AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider
                symbol = AwsRuntimeTypes.Types.CredentialsProvider.toBuilder()
                    .addReference(defaultProvider)
                    .build()
                baseClass = awsClientConfigSymbol
                documentation = """
                    The AWS credentials provider to use for authenticating requests. If not provided a
                    [${symbol?.namespace}.DefaultChainCredentialsProvider] instance will be used.
                """.trimIndent()

                propertyType = ClientConfigPropertyType.RequiredWithDefault("${defaultProvider.name}()")
            }
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> {
        // we can't construct this without the actual package name due to the generated DefaultEndpointResolver symbol
        val endpointResolverProperty = ClientConfigProperty {
            name = "endpointResolver"
            documentation = """
                Determines the endpoint (hostname) to make requests to. When not provided a default
                resolver is configured automatically. This is an advanced client option.
            """.trimIndent()

            val defaultResolver = buildSymbol {
                name = EndpointResolverGenerator.typeName
                namespace = "${ctx.settings.pkg.name}.internal"
            }

            symbol = buildSymbol {
                name = "EndpointResolver"
                namespace(AwsKotlinDependency.AWS_CORE, subpackage = "endpoint")
                reference(defaultResolver)
            }

            propertyType = ClientConfigPropertyType.RequiredWithDefault("${defaultResolver.name}()")
        }

        return listOf(RegionProp, CredentialsProviderProp, endpointResolverProperty)
    }
}
