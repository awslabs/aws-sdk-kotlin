/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.formurl

import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.model.changeNameSuffix
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.traits.OperationInput
import software.amazon.smithy.kotlin.codegen.rendering.serde.FormUrlSerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SdkFieldDescriptorTrait
import software.amazon.smithy.kotlin.codegen.rendering.serde.add
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape

/**
 * A generalized superclass for descriptor generators that follow the "*query" AWS protocols.
 */
abstract class QuerySerdeFormUrlDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null,
) : FormUrlSerdeDescriptorGenerator(ctx, memberShapes) {
    override fun getObjectDescriptorTraits(): List<SdkFieldDescriptorTrait> {
        val traits = super.getObjectDescriptorTraits().toMutableList()

        val objectShape = requireNotNull(ctx.shape)
        if (objectShape.hasTrait<OperationInput>()) {
            // see https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#request-serialization

            // operation inputs are normalized in smithy-kotlin::OperationNormalizer to be "[OperationName]Request"
            val action = objectShape.changeNameSuffix("Request" to "")
            val version = service.version
            traits.add(RuntimeTypes.Serde.SerdeFormUrl.QueryLiteral, "Action".dq(), action.dq())
            traits.add(RuntimeTypes.Serde.SerdeFormUrl.QueryLiteral, "Version".dq(), version.dq())
        }

        return traits
    }
}
