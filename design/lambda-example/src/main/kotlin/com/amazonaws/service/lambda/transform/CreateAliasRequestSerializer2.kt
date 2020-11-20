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
package com.amazonaws.service.lambda.transform

import com.amazonaws.service.lambda.model.CreateAliasRequest2
import software.aws.clientrt.http.HttpMethod
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.feature.HttpSerialize
import software.aws.clientrt.http.feature.SerializationContext
import software.aws.clientrt.http.feature.SerializationProvider
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.request.headers
import software.aws.clientrt.http.request.url
import software.aws.clientrt.serde.SdkFieldDescriptor
import software.aws.clientrt.serde.SdkObjectDescriptor
import software.aws.clientrt.serde.SerialKind
import software.aws.clientrt.serde.serializeStruct

/**
 * This is a hypothetical type that is not modeled in the Lambda service.  Rather,
 * it's used to demonstrate the Union type in Smithy, for which there is not broad
 * service support at the time of writing this file.
 *
 * If/when there is a Union type modeled in Lambda, this file should be replaced
 * with the actual type(s).
 */
class CreateAliasRequestSerializer2(val input: CreateAliasRequest2): HttpSerialize {

    companion object {
        private val DESCRIPTION_FIELD_DESCRIPTOR = SdkFieldDescriptor("Description", SerialKind.String)
        private val FUNCTION_VERSION_DESCRIPTOR = SdkFieldDescriptor("FunctionVersion", SerialKind.String)
        private val NAME_DESCRIPTOR = SdkFieldDescriptor("Name", SerialKind.String)
        private val ROUTING_CONFIG_DESCRIPTOR = SdkFieldDescriptor("RoutingConfig", SerialKind.Struct)
        private val ALIAS_TYPE_FIELD_DESCRIPTOR = SdkFieldDescriptor("AliasType", SerialKind.Struct)

        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build() {
            serialName = "CreateAliasRequest"
            field(DESCRIPTION_FIELD_DESCRIPTOR)
            field(FUNCTION_VERSION_DESCRIPTOR)
            field(NAME_DESCRIPTOR)
            field(ROUTING_CONFIG_DESCRIPTOR)
            field(ALIAS_TYPE_FIELD_DESCRIPTOR)
        }
    }

    override suspend fun serialize(builder: HttpRequestBuilder, serializationContext: SerializationContext) {
        // URI
        builder.method = HttpMethod.POST
        builder.url {
            // NOTE: Individual serializers do not need to concern themselves with protocol/host
            path = "/2015-03-31/functions/${input.functionName}/aliases"
        }

        // Headers
        builder.headers {
            append("Content-Type", "application/json")
        }

        // payload
        val serializer = serializationContext.serializationProvider()
        serializer.serializeStruct(OBJ_DESCRIPTOR) {
            input.name?.let { field(NAME_DESCRIPTOR, it) }
            input.functionVersion?.let { field(FUNCTION_VERSION_DESCRIPTOR, it) }

            // optional fields
            input.description?.let { field(DESCRIPTION_FIELD_DESCRIPTOR, it) }
            input.routingConfig?.let { map ->
                mapField(ROUTING_CONFIG_DESCRIPTOR) {
                    map.entries.forEach{ entry(it.key, it.value) }
                }
            }

            input.aliasType?.let { field(ALIAS_TYPE_FIELD_DESCRIPTOR, AliasTypeSerializer(it)) }
        }

        builder.body = ByteArrayContent(serializer.toByteArray())
    }
}
