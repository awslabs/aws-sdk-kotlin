/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.endpointPrefix
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.node.StringNode
import java.util.*

/**
 * Generates a per/service endpoint resolver (internal to the generated SDK) using endpoints.json
 * @param endpointData Parsed endpoints.json [ObjectNode]
 */
class AwsEndpointResolverGenerator(private val endpointData: ObjectNode) {
    companion object {
        const val typeName = "DefaultEndpointResolver"
    }

    // Symbols which should be imported
    private val endpointResolverSymbols = setOf(
        AwsRuntimeTypes.Endpoint.CredentialScope,
        AwsRuntimeTypes.Endpoint.Internal.EndpointDefinition,
        AwsRuntimeTypes.Endpoint.Internal.Partition,
        AwsRuntimeTypes.Endpoint.Internal.resolveEndpoint,
    )

    fun render(ctx: ProtocolGenerator.GenerationContext) {
        ctx.delegator.useFileWriter("$typeName.kt", "${ctx.settings.pkg.name}.internal") {
            renderResolver(it)
            renderInternalEndpointsModel(ctx, it)
        }
    }

    private fun renderResolver(writer: KotlinWriter) {
        writer.addImport(AwsRuntimeTypes.Endpoint.AwsEndpointResolver)
        writer.addImport(AwsRuntimeTypes.Endpoint.AwsEndpoint)
        writer.addImport(AwsRuntimeTypes.Endpoint.Internal.resolveEndpoint)
        writer.addImport(AwsRuntimeTypes.Core.ClientException)

        writer.openBlock("internal class $typeName : AwsEndpointResolver {", "}") {
            writer.openBlock("override suspend fun resolve(service: String, region: String): AwsEndpoint {", "}") {
                writer.write("return resolveEndpoint(servicePartitions, region) ?: throw ClientException(#S)", "unable to resolve endpoint for region: \$region")
            }
        }
    }

    /**
     * Renders the partition data for this service
     */
    private fun renderInternalEndpointsModel(ctx: ProtocolGenerator.GenerationContext, writer: KotlinWriter) {
        val partitionsData = endpointData.expectArrayMember("partitions").getElementsAs(Node::expectObjectNode)

        val comparePartitions = object : Comparator<PartitionNode> {
            override fun compare(x: PartitionNode, y: PartitionNode): Int {
                // always sort standard aws partition first
                if (x.id == "aws") return -1
                return x.id.compareTo(y.id)
            }
        }

        val partitions = partitionsData.map {
            PartitionNode(ctx.service.endpointPrefix, it)
        }.sortedWith(comparePartitions)

        writer.addImport(endpointResolverSymbols)
        writer.write("")
        writer.openBlock("private val servicePartitions = listOf(", ")") {
            partitions.forEach { renderPartition(writer, it) }
        }
    }

    private fun renderPartition(writer: KotlinWriter, partitionNode: PartitionNode) {
        writer.openBlock("Partition(", "),") {
            writer.write("id = #S,", partitionNode.id)
                .write("regionRegex = Regex(#S),", partitionNode.config.expectStringMember("regionRegex").value)
                .write("partitionEndpoint = #S,", partitionNode.partitionEndpoint ?: "")
                .write("isRegionalized = #L,", partitionNode.partitionEndpoint == null)
                .openBlock("defaults = EndpointDefinition(", "),") {
                    renderEndpointDefinition(writer, partitionNode.defaults)
                }
                .openBlock("endpoints = mapOf(", ")") {
                    partitionNode.endpoints.members.forEach {
                        val definitionNode = it.value.expectObjectNode()
                        if (definitionNode.members.isEmpty()) {
                            writer.write("#S to EndpointDefinition(),", it.key.value)
                        } else {
                            writer.openBlock("#S to EndpointDefinition(", "),", it.key.value) {
                                renderEndpointDefinition(writer, it.value.expectObjectNode())
                            }
                        }
                    }
                }
        }
    }

    private fun renderEndpointDefinition(writer: KotlinWriter, endpointNode: ObjectNode) {
        endpointNode.getStringMember("hostname").ifPresent {
            writer.write("hostname = #S,", it)
        }

        endpointNode.getArrayMember("protocols").ifPresent {
            writer.writeInline("protocols = listOf(")
            it.forEach { writer.writeInline("#S, ", it.expectStringNode().value) }
            writer.write("),")
        }

        endpointNode.getObjectMember("credentialScope").ifPresent {
            writer.writeInline("credentialScope = CredentialScope(")
            it.getStringMember("region").ifPresent {
                writer.writeInline("region = #S,", it.value)
            }
            it.getStringMember("service").ifPresent {
                writer.writeInline("service= #S", it.value)
            }
            writer.write("),")
        }

        endpointNode.getArrayMember("signatureVersions").ifPresent {
            writer.writeInline("signatureVersions = listOf(")
            it.forEach { writer.writeInline("#S, ", it.expectStringNode().value) }
            writer.write("),")
        }
    }

    /**
     * Represents a partition from endpoints.json
     */
    private class PartitionNode(endpointPrefix: String, val config: ObjectNode) {
        // the partition id/name (e.g. "aws")
        val id = config.expectStringMember("partition").value

        // the node associated with [endpointPrefix] (or empty node)
        val service: ObjectNode = config
            .getObjectMember("services").orElse(Node.objectNode())
            .getObjectMember(endpointPrefix).orElse(Node.objectNode())

        // endpoints belonging to the service with the given [endpointPrefix] (or empty node)
        val endpoints: ObjectNode = service.getObjectMember("endpoints").orElse(Node.objectNode())

        val dnsSuffix: String = config.expectStringMember("dnsSuffix").value

        // service specific defaults
        val defaults: ObjectNode
        init {

            val partitionDefaults = config.expectObjectMember("defaults")
            val serviceDefaults = service.getObjectMember("defaults").orElse(Node.objectNode())
            val mergedDefaults = partitionDefaults.merge(serviceDefaults)

            val hostnameTemplate = mergedDefaults.expectStringMember("hostname").value
                .replace("{service}", endpointPrefix)
                .replace("{dnsSuffix}", dnsSuffix)

            defaults = mergedDefaults.withMember("hostname", hostnameTemplate)
        }

        // regionalized services always use regionalized endpoints
        val partitionEndpoint: String? = if (service.getBooleanMemberOrDefault("isRegionalized", true)) {
            null
        } else {
            service.getStringMember("partitionEndpoint").map(StringNode::getValue).getOrNull()
        }
    }
}
