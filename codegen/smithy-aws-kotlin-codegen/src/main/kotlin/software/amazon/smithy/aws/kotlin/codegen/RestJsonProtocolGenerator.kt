/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinDependency
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.model.traits.TimestampFormatTrait

class JsonSerdeFeature : HttpSerde("JsonSerdeProvider") {
    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        val jsonSerdeSymbol = Symbol.builder()
            .name("JsonSerdeProvider")
            .namespace(KotlinDependency.CLIENT_RT_SERDE_JSON.namespace, ".")
            .addDependency(KotlinDependency.CLIENT_RT_SERDE_JSON)
            .build()
        writer.addImport(jsonSerdeSymbol, "")
    }
}

/**
 * Shared base protocol generator for all AWS JSON protocol variants
 */
abstract class RestJsonProtocolGenerator : HttpBindingProtocolGenerator() {
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {
        val ignoredTests = setOf(
            "RestJsonListsSerializeNull", // TODO - sparse lists not supported - this test needs removed
            "RestJsonSerializesNullMapValues", // TODO - sparse maps not supported - this test needs removed
            // FIXME - document type not fully supported yet
            "InlineDocumentInput",
            "InlineDocumentAsPayloadInput",
            "InlineDocumentOutput",
            "InlineDocumentAsPayloadInputOutput"
        )

        val requestTestBuilder = HttpProtocolUnitTestRequestGenerator.Builder()
        val responseTestBuilder = HttpProtocolUnitTestResponseGenerator.Builder()
        HttpProtocolTestGenerator(
            ctx,
            requestTestBuilder,
            responseTestBuilder,
            ignoredTests
        ).generateProtocolTests()
    }

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx).toMutableList()
        val jsonFeatures = listOf(JsonSerdeFeature())
        features.addAll(jsonFeatures)
        return features
    }
}
