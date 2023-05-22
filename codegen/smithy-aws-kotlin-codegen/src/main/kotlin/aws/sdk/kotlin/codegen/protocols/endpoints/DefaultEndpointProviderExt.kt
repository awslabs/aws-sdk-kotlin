/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.ExpressionRenderer
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression
import java.util.*

val awsEndpointFunctions = mapOf(
    "aws.parseArn" to AwsRuntimeTypes.Endpoint.Functions.parseArn,
    "aws.isVirtualHostableS3Bucket" to AwsRuntimeTypes.Endpoint.Functions.isVirtualHostableS3Bucket,
)

val awsEndpointPropertyRenderers = mapOf(
    "authSchemes" to ::renderAuthSchemes,
)

// valid auth scheme names that can appear in a smithy endpoint's properties
private val validAuthSchemeNames = setOf("sigv4", "sigv4a")

private fun String.toSigningContextClassName(): String? =
    when (this) {
        "sigv4" -> "SigV4"
        "sigv4a" -> "SigV4A"
        else -> null
    }

private fun renderAuthSchemes(writer: KotlinWriter, authSchemes: Expression, expressionRenderer: ExpressionRenderer) {
    writer.writeInline("#T to ", RuntimeTypes.SmithyClient.Endpoints.SigningContextAttributeKey)
    writer.withBlock("listOf(", ")") {
        authSchemes.toNode().expectArrayNode().forEach {
            val scheme = it.expectObjectNode()
            val schemeName = scheme.expectStringMember("name").value
            val className = schemeName.toSigningContextClassName() ?: return@forEach

            withBlock("#T.#L(", "),", RuntimeTypes.SmithyClient.Endpoints.SigningContext, className) {
                // we delegate back to the expression visitor for each of these fields because it's possible to
                // encounter template strings throughout

                writeInline("signingName = ")
                renderOrElse(expressionRenderer, scheme.getStringMember("signingName"), "null")

                writeInline("disableDoubleEncoding = ")
                renderOrElse(expressionRenderer, scheme.getBooleanMember("disableDoubleEncoding"), "false")

                when (schemeName) {
                    "sigv4" -> renderSigV4Fields(writer, scheme, expressionRenderer)
                    "sigv4a" -> renderSigV4AFields(writer, scheme, expressionRenderer)
                }
            }
        }
    }
}

private fun KotlinWriter.renderOrElse(
    expressionRenderer: ExpressionRenderer,
    optionalNode: Optional<out Node>,
    whenNullValue: String,
) {
    val nullableNode = optionalNode.getOrNull()
    when (nullableNode) {
        null -> writeInline(whenNullValue)
        else -> expressionRenderer.renderExpression(Expression.fromNode(nullableNode))
    }
    write(",")
}

private fun renderSigV4Fields(writer: KotlinWriter, scheme: ObjectNode, expressionRenderer: ExpressionRenderer) {
    writer.writeInline("signingRegion = ")
    writer.renderOrElse(expressionRenderer, scheme.getStringMember("signingRegion"), "null")
}

private fun renderSigV4AFields(writer: KotlinWriter, scheme: ObjectNode, expressionRenderer: ExpressionRenderer) {
    writer.writeInline("signingRegionSet = ")
    expressionRenderer.renderExpression(Expression.fromNode(scheme.expectArrayMember("signingRegionSet")))
    writer.write(",")
}
