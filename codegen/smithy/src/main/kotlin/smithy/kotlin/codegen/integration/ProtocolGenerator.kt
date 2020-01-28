/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package smithy.kotlin.codegen.integration

import smithy.kotlin.codegen.ApplicationProtocol
import smithy.kotlin.codegen.KotlinSettings
import smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.utils.CaseUtils

/**
 * Smithy protocol code generators.
 */
interface ProtocolGenerator {
    /**
     * Gets the name of the protocol.
     *
     * This is the same name used in Smithy models on the "protocols"
     * trait (e.g., "aws.rest-json-1.1").
     *
     * @return Returns the protocol name.
     */
    val name: String

    /**
     * Creates an application protocol for the generator.
     *
     * @return Returns the created application protocol.
     */
    val applicationProtocol: ApplicationProtocol

    /**
     * Determines if two protocol generators are compatible at the
     * application protocol level, meaning they both use HTTP, or MQTT
     * for example.
     *
     * Two protocol implementations are considered compatible if the
     * [ApplicationProtocol.equals] method of [.getApplicationProtocol]
     * returns true when called with `other`. The default implementation
     * should work for most interfaces, but may be overridden for more in-depth
     * handling of things like minor version incompatibilities.
     *
     * By default, if the application protocols are considered equal, then
     * `other` is returned.
     *
     * @param service Service being generated.
     * @param protocolGenerators Other protocol generators that are being generated.
     * @param other Protocol generator to resolve against.
     * @return Returns the resolved application protocol object.
     */
    fun resolveApplicationProtocol(
        service: ServiceShape,
        protocolGenerators: Collection<ProtocolGenerator>,
        other: ApplicationProtocol
    ): ApplicationProtocol {
        if (applicationProtocol != other) {
            val protocolNames = protocolGenerators.asSequence()
                .map { it.name }
                .sorted()
                .joinToString()
            throw CodegenException(
                "All of the protocols generated for a service must be runtime compatible, but "
                        + "protocol `$name` is incompatible with other application protocols: $protocolNames. Please pick a "
                        + "set of compatible protocols using the `protocols` option when generating ${service.id}."
            )
        }
        return other
    }

    /**
     * Generates any standard code for service request/response serde.
     *
     * @param context Serde context.
     */
    fun generateSharedComponents(context: GenerationContext) {}

    /**
     * Generates the code used to serialize the shapes of a service
     * for requests.
     *
     * @param context Serialization context.
     */
    fun generateRequestSerializers(context: GenerationContext)

    /**
     * Generates the code used to deserialize the shapes of a service
     * for responses.
     *
     * @param context Deserialization context.
     */
    fun generateResponseDeserializers(context: GenerationContext)

    /**
     * Context object used for service serialization and deserialization.
     */
    class GenerationContext {
        var settings: KotlinSettings? = null
        var model: Model? = null
        var service: ServiceShape? = null
        var symbolProvider: SymbolProvider? = null
        var writer: KotlinWriter? = null
        var integrations: List<KotlinIntegration>? = null
        var protocolName: String? = null
    }

    companion object {
        /**
         * Sanitizes the name of the protocol so it can be used as a symbol
         * in TypeScript.
         *
         *
         * For example, the default implementation converts "." to "_",
         * and converts "-" to become camelCase separated words. This means
         * that "aws.rest-json-1.1" becomes "Aws_RestJson1_1".
         *
         * @param name Name of the protocol to sanitize.
         * @return Returns the sanitized name.
         */
        fun getSanitizedName(name: String): String {
            val result = name.replace(".", "_")
            return CaseUtils.toCamelCase(result, true, '-')
        }

        /**
         * Generates the name of a serializer function for shapes of a service.
         *
         * @param symbol The symbol the serializer function is being generated for.
         * @param protocol Name of the protocol being generated.
         * @return Returns the generated function name.
         */
        fun getSerFunctionName(
            symbol: Symbol,
            protocol: String
        ): String? {
            // e.g., serializeAws_restJson1_1ExecuteStatement
            var functionName: String? = "serialize" + getSanitizedName(protocol)

            // These need intermediate serializers, so generate a separate name.
            val shape =
                symbol.expectProperty(
                    "shape",
                    Shape::class.java
                )
            functionName += if (shape.isListShape || shape.isSetShape || shape.isMapShape) {
                shape.id.name
            } else {
                symbol.name
            }
            return functionName
        }

        /**
         * Generates the name of a deserializer function for shapes of a service.
         *
         * @param symbol The symbol the deserializer function is being generated for.
         * @param protocol Name of the protocol being generated.
         * @return Returns the generated function name.
         */
        fun getDeserFunctionName(
            symbol: Symbol,
            protocol: String
        ): String? {
            // e.g., deserializeAws_restJson1_1ExecuteStatement
            var functionName: String? = "deserialize" + getSanitizedName(protocol)

            // These need intermediate serializers, so generate a separate name.
            val shape =
                symbol.expectProperty(
                    "shape",
                    Shape::class.java
                )
            functionName += if (shape.isListShape || shape.isSetShape || shape.isMapShape) {
                shape.id.name
            } else {
                symbol.name
            }
            return functionName
        }
    }
}