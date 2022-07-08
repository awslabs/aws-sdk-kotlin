/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.protocols.core.AwsEndpointResolverGenerator
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.boxed
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.rendering.*
import software.amazon.smithy.model.shapes.ServiceShape

class AwsServiceConfigIntegration : KotlinIntegration {
    companion object {
        val RegionProp: ClientConfigProperty = ClientConfigProperty {
            name = "region"
            symbol = KotlinTypes.String.toBuilder().boxed().build()
            documentation = """
                    AWS region to make requests to
            """.trimIndent()
            propertyType = ClientConfigPropertyType.Required()
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
                    "val #1L: #2T = builder.#1L?.borrow() ?: #3T()",
                    prop.propertyName,
                    prop.symbol,
                    AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider
                )
            })

            additionalImports = listOf(
                AwsRuntimeTypes.Config.Credentials.borrow,
                AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider
            )
        }
    }

    private val overrideServiceCompanionObjectWriter = SectionWriter { writer, _ ->
        // override the service client companion object for how a client is constructed
        val serviceSymbol: Symbol = writer.getContextValue(ServiceGenerator.SectionServiceCompanionObject.ServiceSymbol)
        writer.withBlock("companion object {", "}") {
            withBlock(
                "operator fun invoke(block: Config.Builder.() -> Unit): #L {",
                "}",
                serviceSymbol.name
            ) {
                write("val config = Config.Builder().apply(block).build()")
                write("return Default${serviceSymbol.name}(config)")
            }

            write("")
            write("operator fun invoke(config: Config): ${serviceSymbol.name} = Default${serviceSymbol.name}(config)")

            // generate a convenience init to resolve a client from the current environment
            write("")
            dokka {
                write("Construct a [${serviceSymbol.name}] by resolving the configuration from the current environment.")
            }
            writer.withBlock(
                "suspend fun fromEnvironment(block: (Config.Builder.() -> Unit)? = null): #T {",
                "}",
                serviceSymbol
            ) {
                write("val builder = Config.Builder()")
                write("if (block != null) builder.apply(block)")

                addImport(AwsRuntimeTypes.Config.Region.resolveRegion)
                write("builder.region = builder.region ?: #T()", AwsRuntimeTypes.Config.Region.resolveRegion)
                write("return Default${serviceSymbol.name}(builder.build())")
            }
        }
    }

    private val overrideServiceConfigObjectWriter = SectionWriter { writer, _ ->
        val ctx: RenderingContext<ServiceShape> =
            writer.getContextValue(ServiceGenerator.SectionServiceConfig.RenderingContext)

        // We have to replace the default endpoint resolver with an AwsEndpointResolver
        val autoDetectedProps = ClientConfigGenerator.detectDefaultProps(ctx)
            .filter { it.propertyName != KotlinClientRuntimeConfigProperty.EndpointResolver.propertyName }
            .toTypedArray()

        ClientConfigGenerator(ctx, detectDefaultProps = false, properties = autoDetectedProps).render()
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(
            SectionWriterBinding(ServiceGenerator.SectionServiceCompanionObject, overrideServiceCompanionObjectWriter),
            SectionWriterBinding(ServiceGenerator.SectionServiceConfig, overrideServiceConfigObjectWriter),
        )

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> {
        // we can't construct this without the actual package name due to the generated DefaultEndpointResolver symbol
        val endpointResolverProperty = ClientConfigProperty {
            name = "endpointResolver"
            documentation = """
                Determines the endpoint (hostname) to make requests to. When not provided a default
                resolver is configured automatically. This is an advanced client option.
            """.trimIndent()

            val defaultResolver = buildSymbol {
                name = AwsEndpointResolverGenerator.typeName
                namespace = "${ctx.settings.pkg.name}.internal"
            }
            symbol = AwsRuntimeTypes.Endpoint.AwsEndpointResolver
            propertyType = ClientConfigPropertyType.RequiredWithDefault("${defaultResolver.name}()")
            additionalImports = listOf(defaultResolver)
        }

        return listOf(RegionProp, CredentialsProviderProp, endpointResolverProperty)
    }
}
