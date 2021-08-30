/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.json

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpFeatureMiddleware
import software.amazon.smithy.model.shapes.ShapeId

/**
 * Configure the AwsJsonProtocol middleware
 * @param protocolVersion The AWS JSON protocol version (e.g. "1.0", "1.1", etc)
 */
class AwsJsonProtocolMiddleware(
    private val serviceShapeId: ShapeId,
    private val protocolVersion: String
) : HttpFeatureMiddleware() {
    override val name: String = "AwsJsonProtocol"
    override val order: Byte = 10

    override fun renderConfigure(writer: KotlinWriter) {
        val awsJsonProtocolSymbol = buildSymbol {
            name = "AwsJsonProtocol"
            namespace(AwsKotlinDependency.AWS_JSON_PROTOCOLS)
        }

        writer.addImport(awsJsonProtocolSymbol)
        writer.write("serviceShapeName = #S", serviceShapeId.name)
        writer.write("version = #S", protocolVersion)
    }
}
