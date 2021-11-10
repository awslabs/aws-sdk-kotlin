/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.sts

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * STS needs to have the auth trait manually set to []
 *
 * See https://github.com/awslabs/aws-sdk-kotlin/issues/280
 */
class StsDisableAuthForOperations : KotlinIntegration {

    private val optionalAuthOperations = setOf(
        ShapeId.from("com.amazonaws.sts#AssumeRoleWithSAML"),
        ShapeId.from("com.amazonaws.sts#AssumeRoleWithWebIdentity")
    )

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId == "STS"

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model =
        ModelTransformer.create()
            .mapShapes(model) {
                if (optionalAuthOperations.contains(it.id) && it is OperationShape) {
                    it.toBuilder().addTrait(AuthTrait(emptySet())).build()
                } else {
                    it
                }
            }
}
