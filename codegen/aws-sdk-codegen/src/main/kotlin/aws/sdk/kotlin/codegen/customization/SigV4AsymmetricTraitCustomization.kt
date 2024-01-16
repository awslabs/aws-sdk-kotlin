/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * Adds the sigV4A trait to S3 and event bridge. These services don't model their sigV4A usage
 */
class SigV4AsymmetricTraitCustomization : KotlinIntegration {
    override val order: Byte = -60

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val sdkId = model.expectShape<ServiceShape>(settings.service).sdkId.lowercase() // FIXME don't use sdkId
        return sdkId == "s3" || sdkId == "eventbridge" || sdkId == "cloudfront keyvaluestore"
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model = ModelTransformer.create()
        .mapShapes(model) { shape ->
            if (shape.isServiceShape) {
                val authSchemes: MutableSet<ShapeId> =
                    (shape as ServiceShape).getTrait<AuthTrait>()?.let {
                        val modeledAuthSchemes = it.valueSet
                        modeledAuthSchemes.add(SigV4ATrait.ID)
                        modeledAuthSchemes.add(SigV4Trait.ID)
                        modeledAuthSchemes
                    } ?: mutableSetOf(SigV4ATrait.ID, SigV4Trait.ID)

                // SigV4A trait name is based on these rules: https://smithy.io/2.0/aws/aws-auth.html?highlight=sigv4#aws-auth-sigv4a-trait
                val sigV4ATraitNameProperty =
                    shape.getTrait<ServiceTrait>()?.let { it.arnNamespace }
                        ?: shape.getTrait<SigV4Trait>()?.let { it.name }
                        ?: throw Exception("Service (${shape.id}) is missing ARN namespace. Please report to AWS.")

                shape
                    .toBuilder()
                    .addTrait(
                        SigV4ATrait
                            .builder()
                            .name(sigV4ATraitNameProperty)
                            .build(),
                    )
                    .addTrait(
                        AuthTrait(authSchemes),
                    )
                    .build()
            } else {
                shape
            }
        }
}
