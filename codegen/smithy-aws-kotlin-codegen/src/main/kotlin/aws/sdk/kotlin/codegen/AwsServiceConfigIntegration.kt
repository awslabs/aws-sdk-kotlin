/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.boxed
import software.amazon.smithy.kotlin.codegen.rendering.*
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType

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

        // FIXME - should fips and dual stack props be defined on one of our AWS SDK client config interfaces (e.g. `AwsSdkConfig`) if they apply to every AWS SDK Kotlin service client generated?

        val UseFipsProp: ConfigProperty = ConfigProperty.Boolean(
            "useFips",
            defaultValue = false,
            documentation = """
                Flag to toggle whether to use [FIPS](https://aws.amazon.com/compliance/fips/) endpoints when making requests.
            """.trimIndent(),
        )

        val UseDualStackProp: ConfigProperty = ConfigProperty.Boolean(
            "useDualStack",
            defaultValue = false,
            documentation = """
                Flag to toggle whether to use dual-stack endpoints when making requests.
            """.trimIndent(),
        )

        val EndpointUrlProp = ConfigProperty {
            name = "endpointUrl"
            symbol = RuntimeTypes.Core.Net.Url.toBuilder().boxed().build()
            documentation = """
                A custom endpoint to use when making requests.
            """.trimIndent()
            propertyType = ConfigPropertyType.SymbolDefault
        }
    }

    private val overrideServiceCompanionObjectWriter = SectionWriter { writer, _ ->
        // override the service client companion object for how a client is constructed
        val serviceSymbol = writer.getContextValue(ServiceClientGenerator.Sections.CompanionObject.ServiceSymbol)
        writer.withBlock(
            "public companion object : #T<Config, Config.Builder, #T, Builder>() {",
            "}",
            AwsRuntimeTypes.Config.AbstractAwsSdkClientFactory,
            serviceSymbol,
        ) {
            write("@#T", KotlinTypes.Jvm.JvmStatic)
            write("override fun builder(): Builder = Builder()")
        }
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(
            SectionWriterBinding(ServiceClientGenerator.Sections.CompanionObject, overrideServiceCompanionObjectWriter),
        )

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(
            RegionProp,
            CredentialsProviderProp,
            UseFipsProp,
            UseDualStackProp,
            EndpointUrlProp,
        )
}
