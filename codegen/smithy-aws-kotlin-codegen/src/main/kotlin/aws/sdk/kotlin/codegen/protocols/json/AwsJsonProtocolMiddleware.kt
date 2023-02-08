/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.json

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ShapeId

/**
 * Configure the AwsJsonProtocol middleware
 * @param protocolVersion The AWS JSON protocol version (e.g. "1.0", "1.1", etc)
 */
class AwsJsonProtocolMiddleware(
    private val serviceShapeId: ShapeId,
    private val protocolVersion: String,
) : ProtocolMiddleware {
    override val name: String = "AwsJsonProtocol"
    override val order: Byte = 10

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write("op.install(#T(#S, #S))", RuntimeTypes.AwsJsonProtocols.AwsJsonProtocol, serviceShapeId.name, protocolVersion)
    }
}
