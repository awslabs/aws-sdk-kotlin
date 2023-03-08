/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.boxed
import software.amazon.smithy.kotlin.codegen.rendering.*
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.kotlin.codegen.rendering.util.RuntimeConfigProperty

class AwsServiceConfigIntegration : KotlinIntegration {
    companion object {
        val RegionProp: ConfigProperty = ConfigProperty {
            name = "region"
            symbol = KotlinTypes.String.toBuilder().boxed().build()
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

        // FIXME - this should be registered based on auth scheme in model
        val CredentialsProviderProp: ConfigProperty = ConfigProperty {
            symbol = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProvider
            documentation = """
                The AWS credentials provider to use for authenticating requests. If not provided a
                [${AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider}] instance will be used.
                NOTE: The caller is responsible for managing the lifetime of the provider when set. The SDK
                client will not close it when the client is closed.
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(render = { prop, writer ->
                writer.write(
                    "public val #1L: #2T = builder.#1L ?: #3T(httpClientEngine = httpClientEngine, region = region).#4T()",
                    prop.propertyName,
                    prop.symbol,
                    AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider,
                    AwsRuntimeTypes.Config.Credentials.manage,
                )
            })
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
            symbol = RuntimeTypes.Core.Net.Url.toBuilder().boxed().build()
            documentation = """
                A custom endpoint to use when making requests.
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
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(
            SectionWriterBinding(ServiceClientGenerator.Sections.CompanionObject, ServiceClientCompanionObjectWriter()),
        )

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(
            RegionProp,
            CredentialsProviderProp,
            UseFipsProp,
            UseDualStackProp,
            EndpointUrlProp,
            AwsRetryPolicy,
        )
}
