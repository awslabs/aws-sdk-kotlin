/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * Modify the [AuthTrait] of the CloudFront KeyValueStore service, placing SigV4A before SigV4 in the
 * prioritized list.
 */
class CloudFrontKeyValueStoreSigV4APrioritizationCustomization : KotlinIntegration {
    // Runs after SigV4AsymmetricTraitCustomization (-60) and before `SigV4AsymmetricAuthSchemeIntegration`(-50) and `SigV4AuthSchemeIntegration` (-50)
    override val order: Byte = -55

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = settings.sdkId.lowercase() == "cloudfront keyvaluestore"

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model =
        ModelTransformer.create().mapShapes(model) { shape ->
            when (shape.isServiceShape) {
                true -> {
                    val builder = (shape as ServiceShape).toBuilder()
                    builder.removeTrait(AuthTrait.ID) // remove existing auth trait

                    // add a new auth trait with SigV4A in front
                    val authTrait = AuthTrait(mutableSetOf(SigV4ATrait.ID, SigV4Trait.ID))
                    builder.addTrait(authTrait)

                    builder.build()
                }
                false -> shape
            }
        }
}
