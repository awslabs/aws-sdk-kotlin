/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.ExpressionRenderer
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression

val awsEndpointFunctions = mapOf(
    "aws.parseArn" to AwsRuntimeTypes.Endpoint.Functions.parseArn,
    "aws.isVirtualHostableS3Bucket" to AwsRuntimeTypes.Endpoint.Functions.isVirtualHostableS3Bucket,
)

val awsEndpointPropertyRenderers = mapOf(
    "authSchemes" to ::renderAuthSchemes,
)

// valid auth scheme names that can appear in a smithy endpoint's properties
private val validAuthSchemeNames = setOf("sigv4", "sigv4a")

private fun String.toAuthSchemeClassName(): String? =
    when (this) {
        "sigv4" -> "SigV4"
        "sigv4a" -> "SigV4A"
        else -> null
    }

private fun renderAuthSchemes(writer: KotlinWriter, authSchemes: Expression, expressionRenderer: ExpressionRenderer) {
    writer.withBlock("set(", ")") {
        write("#T,", AwsRuntimeTypes.Endpoint.AuthSchemesAttributeKey)
        withBlock("listOf(", "),") {
            authSchemes.toNode().expectArrayNode().forEach {
                val scheme = it.expectObjectNode()
                val schemeName = scheme.expectStringMember("name").value
                val className = schemeName.toAuthSchemeClassName() ?: return@forEach

                withBlock("#T.#L(", "),", AwsRuntimeTypes.Endpoint.AuthScheme, className) {
                    // we delegate back to the expression visitor for each of these fields because it's possible to
                    // encounter template strings throughout

                    writeInline("signingName = ")
                    scheme.getStringMember("signingName").ifPresentOrElse({ node ->
                        expressionRenderer.renderExpression(Expression.fromNode(node))
                        write(",")
                    },) {
                        writeInline("null,")
                    }

                    writer.writeInline("disableDoubleEncoding = ")
                    scheme.getBooleanMember("disableDoubleEncoding").ifPresentOrElse({ node ->
                        expressionRenderer.renderExpression(Expression.fromNode(node))
                        write(",")
                    },) {
                        writeInline("false,")
                    }

                    when (schemeName) {
                        "sigv4" -> renderSigV4Fields(writer, scheme, expressionRenderer)
                        "sigv4a" -> renderSigV4AFields(writer, scheme, expressionRenderer)
                    }
                }
            }
        }
    }
}

private fun renderSigV4Fields(writer: KotlinWriter, scheme: ObjectNode, expressionRenderer: ExpressionRenderer) {
    writer.writeInline("signingRegion = ")
    scheme.getStringMember("signingRegion").ifPresentOrElse({
        expressionRenderer.renderExpression(Expression.fromNode(it))
        writer.write(",")
    },) {
        writer.write("null,")
    }
}

private fun renderSigV4AFields(writer: KotlinWriter, scheme: ObjectNode, expressionRenderer: ExpressionRenderer) {
    writer.writeInline("signingRegionSet = ")
    expressionRenderer.renderExpression(Expression.fromNode(scheme.expectArrayMember("signingRegionSet")))
    writer.write(",")
}
