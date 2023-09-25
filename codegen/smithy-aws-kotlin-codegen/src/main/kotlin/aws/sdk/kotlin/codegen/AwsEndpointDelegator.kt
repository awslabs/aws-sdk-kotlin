/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.protocols.endpoints.*
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.useFileWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.rulesengine.language.EndpointRuleSet
import software.amazon.smithy.rulesengine.traits.EndpointTestCase
import java.io.File

internal const val PARTITIONS_JSON_ENV_VAR = "PARTITIONS_FILE"
internal const val PARTITIONS_JSON_SYS_PROP = "aws.partitions_file"
private const val PARTITIONS_RESOURCE = "aws/sdk/kotlin/codegen/partitions.json"

class AwsEndpointDelegator : EndpointDelegator {

    private fun getPartitionsJson(): String =
        System.getProperty(PARTITIONS_JSON_SYS_PROP)?.let { File(it).readText() }
            ?: System.getenv(PARTITIONS_JSON_ENV_VAR)?.let { File(it).readText() }
            ?: javaClass.classLoader.getResource(PARTITIONS_RESOURCE)?.readText()
            ?: throw CodegenException("could not load partitions.json resource")

    override fun generateEndpointProvider(ctx: ProtocolGenerator.GenerationContext, rules: EndpointRuleSet?) {
        val partitionsData = getPartitionsJson()
        val partitions = Node.parse(partitionsData).expectObjectNode()
        val partitionsSymbol = PartitionsGenerator.getSymbol(ctx.settings)

        ctx.delegator.useFileWriter(partitionsSymbol) {
            PartitionsGenerator(it, partitions).render()
        }

        val paramsSymbol = EndpointParametersGenerator.getSymbol(ctx.settings)
        val providerSymbol = EndpointProviderGenerator.getSymbol(ctx.settings)
        val defaultProviderSymbol = DefaultEndpointProviderGenerator.getSymbol(ctx.settings)

        ctx.delegator.useFileWriter(providerSymbol) {
            EndpointProviderGenerator(it, ctx.settings, providerSymbol, paramsSymbol).render()
        }

        val endpointFunctions = buildMap {
            putAll(awsEndpointFunctions)
            put(
                "aws.partition",
                buildSymbol {
                    name = "partition"
                    namespace = PartitionsGenerator.getSymbol(ctx.settings).namespace
                },
            )
        }
        if (rules != null) {
            ctx.delegator.useFileWriter(defaultProviderSymbol) {
                DefaultEndpointProviderGenerator(it, rules, defaultProviderSymbol, providerSymbol, paramsSymbol, ctx.settings, endpointFunctions, awsEndpointPropertyRenderers).render()
            }
        }
    }

    override fun generateEndpointResolverAdapter(ctx: ProtocolGenerator.GenerationContext) {
        ctx.delegator.useFileWriter(EndpointResolverAdapterGenerator.getSymbol(ctx.settings)) {
            EndpointResolverAdapterGenerator(ctx, it) {
                it.write(
                    "endpoint.#T?.#T(request.context)",
                    RuntimeTypes.SmithyClient.Endpoints.signingContext,
                    RuntimeTypes.Auth.Signing.AwsSigningCommon.mergeInto,
                )
            }.render()
        }
    }

    override fun generateEndpointProviderTests(
        ctx: ProtocolGenerator.GenerationContext,
        tests: List<EndpointTestCase>,
        rules: EndpointRuleSet,
    ) {
        val paramsSymbol = EndpointParametersGenerator.getSymbol(ctx.settings)
        val defaultProviderSymbol = DefaultEndpointProviderGenerator.getSymbol(ctx.settings)
        val testSymbol = DefaultEndpointProviderTestGenerator.getSymbol(ctx.settings)

        ctx.delegator.useTestFileWriter("${testSymbol.name}.kt", testSymbol.namespace) {
            DefaultEndpointProviderTestGenerator(
                it,
                rules,
                tests,
                defaultProviderSymbol,
                paramsSymbol,
                awsEndpointPropertyRenderers,
            ).render()
        }
    }
}
