/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointBusinessMetrics
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Renders the addition of the [BusinessMetricsInterceptor] and endpoint business metrics emitters
 */
class BusinessMetricsIntegration : KotlinIntegration {
    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(EndpointBusinessMetrics, endpointBusinessMetricsSectionWriter),
        )

    private val endpointBusinessMetricsSectionWriter = SectionWriter { writer, _ ->
        writer.write(
            "if (endpoint.attributes.contains(#T)) request.context.#T(#T.SERVICE_ENDPOINT_OVERRIDE)",
            RuntimeTypes.Core.BusinessMetrics.serviceEndpointOverride,
            RuntimeTypes.Core.BusinessMetrics.emitBusinessMetrics,
            RuntimeTypes.Core.BusinessMetrics.BusinessMetrics,
        )

        writer.write(
            "if (endpoint.attributes.contains(#T)) request.context.#T(#T.ACCOUNT_ID_BASED_ENDPOINT)",
            RuntimeTypes.Core.BusinessMetrics.accountIdBasedEndPoint,
            RuntimeTypes.Core.BusinessMetrics.emitBusinessMetrics,
            RuntimeTypes.Core.BusinessMetrics.BusinessMetrics,
        )
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + userAgentBusinessMetricsMiddleware

    private val userAgentBusinessMetricsMiddleware = object : ProtocolMiddleware {
        override val name: String = "UserAgentBusinessMetrics"
        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write(
                "op.interceptors.add(#T())",
                AwsRuntimeTypes.Http.Interceptors.BusinessMetricsInterceptor,
            )
        }
    }
}
