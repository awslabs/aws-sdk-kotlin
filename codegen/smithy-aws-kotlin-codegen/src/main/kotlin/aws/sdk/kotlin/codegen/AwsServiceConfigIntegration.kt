/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.protocols.core.EndpointResolverGenerator
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.boxed
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigPropertyType
import software.amazon.smithy.kotlin.codegen.rendering.ServiceGenerator

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

        private val overrideServiceCompanionObjectWriter = SectionWriter { writer, _ ->
            // override the service client companion object for how a client is constructed
            val serviceSymbol: Symbol = writer.getContextValue(ServiceGenerator.ServiceInterfaceCompanionObject.ServiceSymbol)
            writer.withBlock("companion object {", "}") {
                withBlock(
                    "operator fun invoke(sharedConfig: #T? = null, block: Config.DslBuilder.() -> Unit = {}): #L {",
                    "}",
                    AwsRuntimeTypes.Types.AwsClientConfig,
                    serviceSymbol.name
                ) {
                    withBlock(
                        "val config = Config.BuilderImpl().apply { ",
                        "}.apply(block).build()"
                    ) {
                        write("region = sharedConfig?.region")
                        write("credentialsProvider = sharedConfig?.credentialsProvider")
                    }
                    write("return Default${serviceSymbol.name}(config)")
                }

                // generate a convenience init to resolve a client from the current environment
                listOf(
                    AwsRuntimeTypes.Types.AwsClientConfig,
                    AwsRuntimeTypes.Config.AwsClientConfigLoadOptions,
                    AwsRuntimeTypes.Config.loadFromEnvironment
                ).forEach(writer::addImport)

                write("")
                dokka {
                    write("Construct a [${serviceSymbol.name}] by resolving the configuration from the current environment.")
                    write("NOTE: If you are constructing multiple clients it is more efficient to construct an")
                    write("[#Q] and share the configuration across clients.", AwsRuntimeTypes.Types.AwsClientConfig)
                }
                writer.withBlock(
                    "suspend fun loadFromEnvironment(block: #1T.() -> Unit = {}): #2T {",
                    "}",
                    AwsRuntimeTypes.Config.AwsClientConfigLoadOptions,
                    serviceSymbol
                ) {
                    write(
                        "val sharedConfig = #T.#T(block)",
                        AwsRuntimeTypes.Types.AwsClientConfig,
                        AwsRuntimeTypes.Config.loadFromEnvironment
                    )
                    write("return #T(sharedConfig)", serviceSymbol)
                }
            }
        }
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(SectionWriterBinding(ServiceGenerator.ServiceInterfaceCompanionObject, overrideServiceCompanionObjectWriter))

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
