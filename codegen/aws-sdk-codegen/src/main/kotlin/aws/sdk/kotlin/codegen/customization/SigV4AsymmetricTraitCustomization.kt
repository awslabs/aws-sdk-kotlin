/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.transform.ModelTransformer

// FIXME: Remove services from customization or customization entirely when/if services add sigV4a trait to models
/**
 * Adds the sigV4A trait to services that don't model their sigV4A usage
 * NOTE: Won't add sigV4 trait (services that support sigV4A MUST support sigV4)
 */
class SigV4AsymmetricTraitCustomization : KotlinIntegration {
    // Needs to happen before the `SigV4AsymmetricAuthSchemeIntegration` & `SigV4AuthSchemeIntegration` (-50 & -50)
    override val order: Byte = -60

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        when (settings.sdkId.lowercase()) {
            "s3", "eventbridge", "cloudfront keyvaluestore" -> true
            else -> false
        }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model =
        ModelTransformer.create().mapShapes(model) { shape ->
            when (shape.isServiceShape) {
                true ->
                    (shape as ServiceShape)
                        .toBuilder()
                        .addTraits(
                            mutableSetOf(
                                SigV4ATrait
                                    .builder()
                                    .name(shape.expectTrait<ServiceTrait>().arnNamespace)
                                    .build(),
                                AuthTrait(mutableSetOf(SigV4ATrait.ID, SigV4Trait.ID)),
                            ),
                        )
                        .build()
                false -> shape
            }
        }
}
