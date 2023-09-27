/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.json

import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.rendering.serde.JsonSerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SdkFieldDescriptorTrait
import software.amazon.smithy.kotlin.codegen.rendering.serde.add
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape

class AwsJsonProtocolSerdeDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null,
    supportsJsonNameTrait: Boolean = true,
) : JsonSerdeDescriptorGenerator(ctx, memberShapes, supportsJsonNameTrait) {

    /**
     * Adds a trait to ignore `__type` in union shapes for AWS specific JSON protocols
     * Sometimes the unnecessary trait `__type` is added and needs to be ignored
     *
     * NOTE: Will be ignored unless it's in the model
     *
     * Source: https://github.com/smithy-lang/smithy/pull/1945
     */
    override fun getObjectDescriptorTraits(): List<SdkFieldDescriptorTrait> {
        val traitList = super.getObjectDescriptorTraits().toMutableList()
        if (ctx.shape?.isUnionShape == true) traitList.add(RuntimeTypes.Serde.IgnoreKey, "__type".dq(), "false")
        return traitList
    }
}
