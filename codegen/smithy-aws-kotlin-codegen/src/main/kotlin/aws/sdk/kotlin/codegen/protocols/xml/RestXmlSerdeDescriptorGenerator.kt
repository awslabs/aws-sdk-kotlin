/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.xml

import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.model.isError
import software.amazon.smithy.kotlin.codegen.rendering.serde.SdkFieldDescriptorTrait
import software.amazon.smithy.kotlin.codegen.rendering.serde.XmlSerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.add
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.kotlin.codegen.utils.toggleFirstCharacterCase
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape

/**
 * restXml-specific descriptor generator
 */
class RestXmlSerdeDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null,
) : XmlSerdeDescriptorGenerator(ctx, memberShapes) {
    override fun getFieldDescriptorTraits(
        member: MemberShape,
        targetShape: Shape,
        nameSuffix: String,
    ): List<SdkFieldDescriptorTrait> {
        val traitList = super.getFieldDescriptorTraits(member, targetShape, nameSuffix).toMutableList()

        if (ctx.shape?.isError == true) {
            val serialName = getSerialName(member, nameSuffix)
            if (serialName.equals("message", ignoreCase = true)) {
                // Need to be able to read error messages from "Message" or "message"
                // https://github.com/awslabs/smithy-kotlin/issues/352
                traitList.add(RuntimeTypes.Serde.SerdeXml.XmlAliasName, serialName.toggleFirstCharacterCase().dq())
            }
        }

        return traitList
    }
}
