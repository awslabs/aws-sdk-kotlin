/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.machinelearning

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

class MachineLearningEndpointCustomization : KotlinIntegration {
    // the default resolver middleware will still be present in the operation's execution stack, we just
    // need to ensure that the custom resolver runs _after_ the default
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> = resolved + endpointResolverMiddleware

    private val endpointResolverMiddleware = object : HttpFeatureMiddleware() {
        override val name: String = "ResolvePredictEndpoint"
        override val needsConfiguration: Boolean = false

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            op.id.name == "Predict"

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            super.render(ctx, op, writer)
            writer.addImport(machineLearningSymbol("ResolvePredictEndpoint"))
        }

        private fun machineLearningSymbol(name: String) = buildSymbol {
            this.name = name
            namespace = "aws.sdk.kotlin.services.machinelearning.internal"
        }
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("Machine Learning", ignoreCase = true)
}
