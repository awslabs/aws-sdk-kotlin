/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.json

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Abstract base class that all protocols using JSON as a document format can inherit from
 */
abstract class JsonHttpBindingProtocolGenerator : AwsHttpBindingProtocolGenerator() {

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    /**
     * Flag indicating if the jsonName trait is supported or not. When true the trait is processed when generating
     * serializers and deserializers. When false the member name is used.
     */
    open val supportsJsonNameTrait: Boolean = true

    override fun structuredDataParser(ctx: ProtocolGenerator.GenerationContext): StructuredDataParserGenerator =
        JsonParserGenerator(this, supportsJsonNameTrait = supportsJsonNameTrait)

    override fun structuredDataSerializer(ctx: ProtocolGenerator.GenerationContext): StructuredDataSerializerGenerator =
        JsonSerializerGenerator(this, supportsJsonNameTrait = supportsJsonNameTrait)

    override fun renderDeserializeErrorDetails(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter,
    ) {
        writer.addImport(AwsRuntimeTypes.JsonProtocols.RestJsonErrorDeserializer)
        writer.write("#T.deserialize(response.headers, payload)", AwsRuntimeTypes.JsonProtocols.RestJsonErrorDeserializer)
    }
}
