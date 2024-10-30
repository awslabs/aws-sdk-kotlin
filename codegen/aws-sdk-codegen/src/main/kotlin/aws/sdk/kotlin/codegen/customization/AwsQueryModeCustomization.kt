/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.aws.traits.protocols.AwsQueryCompatibleTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.MutateHeadersMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model

/**
 * Send an extra `x-amzn-query-mode` header with a value of `true` for services which have the [AwsQueryCompatibleTrait] applied.
 */
class AwsQueryModeCustomization : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model
            .getShape(settings.service)
            .get()
            .hasTrait<AwsQueryCompatibleTrait>()

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>): List<ProtocolMiddleware> =
        resolved + MutateHeadersMiddleware(extraHeaders = mapOf("x-amzn-query-mode" to "true"))
}
