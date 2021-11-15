/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * HTTP client interceptor that resolves service endpoints for a single service
 */
class ResolveAwsEndpointMiddleware(private val ctx: ProtocolGenerator.GenerationContext) : ProtocolMiddleware {
    override val name: String = "ResolveAwsEndpoint"
    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val resolverFeatureSymbol = buildSymbol {
            name = "ResolveAwsEndpoint"
            namespace(AwsKotlinDependency.AWS_HTTP, subpackage = "middleware")
        }
        writer.addImport(resolverFeatureSymbol)
        writer.write("op.install(#T(ServiceId, config.endpointResolver))", resolverFeatureSymbol)
    }
}
