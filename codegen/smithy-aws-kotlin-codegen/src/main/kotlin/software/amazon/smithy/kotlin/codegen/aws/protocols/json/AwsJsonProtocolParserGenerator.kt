/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.kotlin.codegen.aws.protocols.json

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.toRenderingContext
import software.amazon.smithy.kotlin.codegen.rendering.serde.JsonParserGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.JsonSerdeDescriptorGenerator
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape

/**
 * Overrides the [JsonParserGenerator] when using `AWS Json 1.0`, `AWS Json 1.1`, and `AWS RestJson 1` protocols.
 *
 * See https://github.com/smithy-lang/smithy/pull/1945
 */
class AwsJsonProtocolParserGenerator(
    private val protocolGenerator: ProtocolGenerator,
    private val supportsJsonNameTrait: Boolean = true,
) : JsonParserGenerator(protocolGenerator, supportsJsonNameTrait) {

    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): JsonSerdeDescriptorGenerator = AwsJsonProtocolSerdeDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members, supportsJsonNameTrait)
}
