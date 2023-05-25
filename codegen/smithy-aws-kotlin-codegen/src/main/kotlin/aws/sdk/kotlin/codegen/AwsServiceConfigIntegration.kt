/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.asNullable
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.model.nullable
import software.amazon.smithy.kotlin.codegen.rendering.*
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.kotlin.codegen.rendering.util.RuntimeConfigProperty
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.traits.HttpBearerAuthTrait

class AwsServiceConfigIntegration : KotlinIntegration {
    companion object {
        val RegionProp: ConfigProperty = ConfigProperty {
            name = "region"
            symbol = KotlinTypes.String.toBuilder().nullable().build()
            baseClass = AwsRuntimeTypes.Core.Client.AwsSdkClientConfig
            useNestedBuilderBaseClass()
            documentation = """
                The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
                [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more
                information
            """.trimIndent()
            propertyType = ConfigPropertyType.Required()
            order = -100
        }

        // override the credentials provider prop registered by the Sigv4AuthSchemeIntegration, updates the
        // documentation and sets a default value for AWS SDK to the default chain.
        val CredentialsProviderProp: ConfigProperty = ConfigProperty {
            symbol = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProvider
            baseClass = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProviderConfig
            useNestedBuilderBaseClass()
            documentation = """
                The AWS credentials provider to use for authenticating requests. If not provided a
                [${AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider}] instance will be used.
                NOTE: The caller is responsible for managing the lifetime of the provider when set. The SDK
                client will not close it when the client is closed.
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(
                render = { prop, writer ->
                    writer.write(
                        "override val #1L: #2T = builder.#1L ?: #3T(httpClient = httpClient, region = region).#4T()",
                        prop.propertyName,
                        prop.symbol,
                        AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider,
                        AwsRuntimeTypes.Config.Credentials.manage,
                    )
                },
            )
        }

        val UseFipsProp: ConfigProperty = ConfigProperty {
            name = "useFips"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to toggle whether to use [FIPS](https://aws.amazon.com/compliance/fips/) endpoints when making requests.
     `          Disabled by default.
            """.trimIndent()
            baseClass = AwsRuntimeTypes.Core.Client.AwsSdkClientConfig
            useNestedBuilderBaseClass()
        }

        val UseDualStackProp: ConfigProperty = ConfigProperty {
            name = "useDualStack"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to toggle whether to use dual-stack endpoints when making requests.
                See [https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoints.html] for more information.
     `          Disabled by default.
            """.trimIndent()
            baseClass = AwsRuntimeTypes.Core.Client.AwsSdkClientConfig
            useNestedBuilderBaseClass()
        }

        val EndpointUrlProp = ConfigProperty {
            name = "endpointUrl"
            symbol = RuntimeTypes.Core.Net.Url.asNullable()
            documentation = """
                A custom endpoint to route requests to. The endpoint set here is passed to the configured
                [endpointProvider], which may inspect and modify it as needed.

                Setting a custom endpointUrl should generally be preferred to overriding the [endpointProvider] and is
                the recommended way to route requests to development or preview instances of a service.

                **This is an advanced config option.**
            """.trimIndent()
            propertyType = ConfigPropertyType.SymbolDefault
        }

        val AwsRetryPolicy = RuntimeConfigProperty
            .RetryPolicy
            .toBuilder()
            .apply {
                propertyType = ConfigPropertyType.RequiredWithDefault("AwsDefaultRetryPolicy")
                additionalImports = listOf(AwsRuntimeTypes.Http.Retries.AwsDefaultRetryPolicy)
            }
            .build()

        // override the bearer token provider registered by BearerTokenAuthSchemeIntegration, updates documentation
        // and configures the default to be the DefaultBearerTokenProviderChain
        val BearerTokenProviderProp = ConfigProperty {
            name = "bearerTokenProvider"
            symbol = RuntimeTypes.Auth.HttpAuth.BearerTokenProvider
            baseClass = RuntimeTypes.Auth.HttpAuth.BearerTokenProviderConfig
            useNestedBuilderBaseClass()
            documentation = """
                The token provider to use for authenticating requests when using [${RuntimeTypes.Auth.HttpAuth.BearerTokenAuthScheme.fullName}].
                If not provided a [${AwsRuntimeTypes.Config.Credentials.DefaultChainBearerTokenProvider}] instance will be used.
                NOTE: The caller is responsible for managing the lifetime of the provider when set. The SDK
                client will not close it when the client is closed.
                
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(
                render = { prop, writer ->
                    writer.write(
                        "override val #1L: #2T = builder.#1L ?: #3T(httpClient = httpClient).#4T()",
                        prop.propertyName,
                        prop.symbol,
                        AwsRuntimeTypes.Config.Credentials.DefaultChainBearerTokenProvider,
                        AwsRuntimeTypes.Config.Credentials.manage,
                    )
                },
            )
        }
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(
            SectionWriterBinding(ServiceClientGenerator.Sections.CompanionObject, ServiceClientCompanionObjectWriter()),
        )

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = buildList {
        add(RegionProp)
        if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.settings.getService(ctx.model))) {
            add(CredentialsProviderProp)
        }

        val serviceIndex = ServiceIndex.of(ctx.model)
        val hasBearerTokenAuth = serviceIndex
            .getAuthSchemes(ctx.settings.service)
            .containsKey(HttpBearerAuthTrait.ID)
        if (hasBearerTokenAuth) {
            add(BearerTokenProviderProp)
        }

        add(UseFipsProp)
        add(UseDualStackProp)
        add(EndpointUrlProp)
        add(AwsRetryPolicy)
    }
}
