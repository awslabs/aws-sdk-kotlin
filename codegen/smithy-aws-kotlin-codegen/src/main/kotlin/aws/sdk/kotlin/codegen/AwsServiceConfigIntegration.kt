/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.boxed
import software.amazon.smithy.kotlin.codegen.rendering.*

class AwsServiceConfigIntegration : KotlinIntegration {
    companion object {
        val RegionProp: ClientConfigProperty = ClientConfigProperty {
            name = "region"
            symbol = KotlinTypes.String.toBuilder().boxed().build()
            documentation = """
                    AWS region to make requests to
            """.trimIndent()
            propertyType = ClientConfigPropertyType.Required()
            order = -100
        }

        val CredentialsProviderProp: ClientConfigProperty = ClientConfigProperty {
            symbol = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProvider
            documentation = """
                The AWS credentials provider to use for authenticating requests. If not provided a
                [${AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider}] instance will be used.
                NOTE: The caller is responsible for managing the lifetime of the provider when set. The SDK
                client will not close it when the client is closed.
            """.trimIndent()

            propertyType = ClientConfigPropertyType.Custom(render = { prop, writer ->
                writer.write(
                    "public val #1L: #2T = builder.#1L?.borrow() ?: #3T(httpClientEngine = httpClientEngine, region = region)",
                    prop.propertyName,
                    prop.symbol,
                    AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider,
                )
            },)

            additionalImports = listOf(
                AwsRuntimeTypes.Config.Credentials.borrow,
                AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider,
            )
        }

        val UseFipsProp: ClientConfigProperty = ClientConfigProperty.Boolean(
            "useFips",
            defaultValue = false,
            documentation = """
                Flag to toggle whether to use [FIPS](https://aws.amazon.com/compliance/fips/) endpoints when making requests.
            """.trimIndent(),
        )

        val UseDualStackProp: ClientConfigProperty = ClientConfigProperty.Boolean(
            "useDualStack",
            defaultValue = false,
            documentation = """
                Flag to toggle whether to use dual-stack endpoints when making requests.
            """.trimIndent(),
        )

        val EndpointUrlProp = ClientConfigProperty {
            name = "endpointUrl"
            symbol = RuntimeTypes.Http.Url.toBuilder().boxed().build()
            documentation = """
                A custom endpoint to use when making requests.
            """.trimIndent()
            propertyType = ClientConfigPropertyType.SymbolDefault
        }
    }

    private val overrideServiceCompanionObjectWriter = SectionWriter { writer, _ ->
        // override the service client companion object for how a client is constructed
        val serviceSymbol: Symbol = writer.getContextValue(ServiceGenerator.SectionServiceCompanionObject.ServiceSymbol)
        writer.withBlock("public companion object {", "}") {
            withBlock(
                "public operator fun invoke(block: Config.Builder.() -> Unit): #L {",
                "}",
                serviceSymbol.name,
            ) {
                write("val config = Config.Builder().apply(block).build()")
                write("return Default${serviceSymbol.name}(config)")
            }

            write("")
            write("public operator fun invoke(config: Config): ${serviceSymbol.name} = Default${serviceSymbol.name}(config)")

            // generate a convenience init to resolve a client from the current environment
            write("")
            dokka {
                write("Construct a [${serviceSymbol.name}] by resolving the configuration from the current environment.")
            }
            writer.withBlock(
                "public suspend fun fromEnvironment(block: (Config.Builder.() -> Unit)? = null): #T {",
                "}",
                serviceSymbol,
            ) {
                write("val builder = Config.Builder()")
                write("if (block != null) builder.apply(block)")

                addImport(AwsRuntimeTypes.Config.Region.resolveRegion)
                write("builder.region = builder.region ?: #T()", AwsRuntimeTypes.Config.Region.resolveRegion)
                write("builder.retryStrategy = builder.retryStrategy ?: #T()", AwsRuntimeTypes.Config.Retries.resolveRetryStrategy)
                write("return Default${serviceSymbol.name}(builder.build())")
            }
        }
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(
            SectionWriterBinding(ServiceGenerator.SectionServiceCompanionObject, overrideServiceCompanionObjectWriter),
        )

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> =
        listOf(
            RegionProp,
            CredentialsProviderProp,
            UseFipsProp,
            UseDualStackProp,
            EndpointUrlProp,
        )
}
