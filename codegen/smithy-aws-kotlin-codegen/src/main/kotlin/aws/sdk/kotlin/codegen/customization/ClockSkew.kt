/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Adds a middleware which applies an interceptor that detects and corrects clock skew
 */
class ClockSkew : KotlinIntegration {
    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + clockSkewMiddleware

    private val clockSkewMiddleware = object : ProtocolMiddleware {
        override val name: String = "ClockSkew"

        override fun renderProperties(writer: KotlinWriter) {
            val interceptorSymbol = RuntimeTypes.AwsProtocolCore.ClockSkewInterceptor
            writer.write("private val clockSkewInterceptor = #T()", interceptorSymbol)
        }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write("op.interceptors.add(clockSkewInterceptor)")
        }
    }
}
