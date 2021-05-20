/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.query

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock

/**
 * AWS Query protocol has asymmetric serializers and deserializers. We generate a SerdeProvider implementation
 * that pulls in the correct formats for each into the internals of the service.
 */
class QuerySerdeProviderGenerator(private val serdeProviderSymbol: Symbol) {
    fun render(writer: KotlinWriter) {
        listOf(
            RuntimeTypes.Serde.SerdeProvider,
            RuntimeTypes.Serde.Serializer,
            RuntimeTypes.Serde.Deserializer,
            RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer,
            RuntimeTypes.Serde.SerdeXml.XmlDeserializer,
        ).forEach { symbol -> writer.addImport(symbol) }

        writer.withBlock("internal class #T : SerdeProvider {", "}", serdeProviderSymbol) {
            writer.write("override fun serializer(): Serializer = #T()", RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer)
            writer.write("override fun deserializer(payload: ByteArray): Deserializer = #T(payload)", RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        }
    }
}
