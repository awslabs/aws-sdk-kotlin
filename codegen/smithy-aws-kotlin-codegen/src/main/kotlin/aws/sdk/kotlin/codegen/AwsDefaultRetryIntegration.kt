/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.protocol.replace
import software.amazon.smithy.kotlin.codegen.retries.StandardRetryMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Adds AWS-specific retry wrappers around operation invocations. This replaces
 * [StandardRetryPolicy][aws.smithy.kotlin.runtime.retries.impl] with
 * [AwsDefaultRetryPolicy][aws.sdk.kotlin.runtime.http.retries].
 */
class AwsDefaultRetryIntegration : KotlinIntegration {
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> = resolved.replace(middleware) { it is StandardRetryMiddleware }

    private val middleware = object : ProtocolMiddleware {
        override val name: String = RuntimeTypes.Http.Middlware.Retry.name

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.addImport(RuntimeTypes.Http.Middlware.Retry)
            writer.addImport(AwsRuntimeTypes.Http.Retries.AwsDefaultRetryPolicy)
            writer.write("op.install(#T(config.retryStrategy, AwsDefaultRetryPolicy))", RuntimeTypes.Http.Middlware.Retry)
        }
    }
}
