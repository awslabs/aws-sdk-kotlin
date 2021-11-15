/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.machinelearning

import aws.sdk.kotlin.codegen.protocols.middleware.ResolveAwsEndpointMiddleware
import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

// FIXME - this is wrong, it's removing the default aws endpoint resolver middleware and only adding back a resolver if the
// protocol middleware is enabled...
class MachineLearningEndpointCustomization : KotlinIntegration {
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> =
        super
            .customizeMiddleware(ctx, resolved)
            .replace(endpointResolverMiddleware) { it is ResolveAwsEndpointMiddleware }

    private val endpointResolverMiddleware = object : ProtocolMiddleware {
        override val name: String = "ResolvePredictEndpoint"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            op.id.name == "Predict"

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val symbol = machineLearningSymbol("ResolvePredictEndpoint")
            writer.addImport(symbol)
            writer.write("op.install(#T())", symbol)
        }

        private fun machineLearningSymbol(name: String) = buildSymbol {
            this.name = name
            namespace = "aws.sdk.kotlin.services.machinelearning.internal"
        }
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("Machine Learning", ignoreCase = true)
}
