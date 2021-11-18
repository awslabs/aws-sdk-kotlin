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
            baseClass = AwsRuntimeTypes.Types.AwsClientConfig
            documentation = """
                    AWS region to make requests to
            """.trimIndent()
            propertyType = ClientConfigPropertyType.Required()
        }

        val CredentialsProviderProp: ClientConfigProperty = ClientConfigProperty {
            symbol = AwsRuntimeTypes.Types.CredentialsProvider
            baseClass = AwsRuntimeTypes.Types.AwsClientConfig
            documentation = """
                The AWS credentials provider to use for authenticating requests. If not provided a
                [${symbol?.namespace}.DefaultChainCredentialsProvider] instance will be used.
            """.trimIndent()

            val defaultProvider = AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider
            propertyType = ClientConfigPropertyType.RequiredWithDefault("${defaultProvider.name}()")
            additionalImports = listOf(defaultProvider)
        }
    }

    private val overrideServiceCompanionObjectWriter = SectionWriter { writer, _ ->
        // override the service client companion object for how a client is constructed
        val serviceSymbol: Symbol = writer.getContextValue(ServiceGenerator.SectionServiceCompanionObject.ServiceSymbol)
        writer.withBlock("companion object {", "}") {
            withBlock(
                "operator fun invoke(sharedConfig: #T? = null, block: Config.Builder.() -> Unit = {}): #L {",
                "}",
                AwsRuntimeTypes.Types.AwsClientConfig,
                serviceSymbol.name
            ) {
                withBlock(
                    "val config = Config.Builder().apply { ",
                    "}.apply(block).build()"
                ) {
                    write("region = sharedConfig?.region")
                    write("credentialsProvider = sharedConfig?.credentialsProvider")
                }
                write("return Default${serviceSymbol.name}(config)")
            }

            write("")
            write("operator fun invoke(config: Config): ${serviceSymbol.name} = Default${serviceSymbol.name}(config)")

            // generate a convenience init to resolve a client from the current environment
            listOf(
                AwsRuntimeTypes.Types.AwsClientConfig,
                AwsRuntimeTypes.Config.AwsClientConfigLoadOptions,
                AwsRuntimeTypes.Config.fromEnvironment
            ).forEach(writer::addImport)

            write("")
            dokka {
                write("Construct a [${serviceSymbol.name}] by resolving the configuration from the current environment.")
                write("NOTE: If you are using multiple AWS service clients you may wish to share the configuration among them")
                write("by constructing a [#Q] and passing it to each client at construction.", AwsRuntimeTypes.Types.AwsClientConfig)
            }
            writer.withBlock(
                "suspend fun fromEnvironment(block: #1T.() -> Unit = {}): #2T {",
                "}",
                AwsRuntimeTypes.Config.AwsClientConfigLoadOptions,
                serviceSymbol
            ) {
                write(
                    "val sharedConfig = #T.#T(block)",
                    AwsRuntimeTypes.Types.AwsClientConfig,
                    AwsRuntimeTypes.Config.fromEnvironment
                )
                write("return #T(sharedConfig)", serviceSymbol)
            }
        }
    }

    private val overrideServiceConfigObjectWriter = SectionWriter { writer, _ ->
        val ctx = writer.getContextValue<RenderingContext<ServiceShape>>(ServiceGenerator.SectionServiceConfig.RenderingContext)

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
