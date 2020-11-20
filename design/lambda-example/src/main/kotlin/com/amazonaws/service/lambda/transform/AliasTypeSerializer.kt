package com.amazonaws.service.lambda.transform

import com.amazonaws.service.lambda.model.*
import software.aws.clientrt.http.feature.HttpSerialize
import software.aws.clientrt.http.feature.SerializationProvider
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.serde.*

/**
 * This is a hypothetical type that is not modeled in the Lambda service.  Rather,
 * it's used to demonstrate the Union type in Smithy, for which there is not broad
 * service support at the time of writing this file.
 *
 * If/when there is a Union type modeled in Lambda, this file should be replaced
 * with the actual type(s).
 */
class AliasTypeSerializer(val input: AliasType) : SdkSerializable {

    companion object {
        private val EXPIRING_ALIAS_TYPE_FIELD_DESCRIPTOR = SdkFieldDescriptor("ExpiringAliasType", SerialKind.String)
        private val REMOTE_ALIAS_TYPE_FIELD_DESCRIPTOR = SdkFieldDescriptor("RemoteAliasType", SerialKind.Long)
        private val MULTI_ALIAS_TYPE_FIELD_DESCRIPTOR = SdkFieldDescriptor("MultiAliasType", SerialKind.List)

        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build() {
            serialName = "AliasType"
            field(EXPIRING_ALIAS_TYPE_FIELD_DESCRIPTOR)
            field(REMOTE_ALIAS_TYPE_FIELD_DESCRIPTOR)
        }
    }

    override fun serialize(serializer: Serializer) {
        serializer.serializeStruct(OBJ_DESCRIPTOR) {
            when (input) {
                is AliasType.ExpiringAliasType -> field(EXPIRING_ALIAS_TYPE_FIELD_DESCRIPTOR, input.value!!)
                is AliasType.RemoteAliasType -> field(REMOTE_ALIAS_TYPE_FIELD_DESCRIPTOR, input.value!!)
                is AliasType.MultiAliasType -> serializer.serializeList(MULTI_ALIAS_TYPE_FIELD_DESCRIPTOR) {
                    for (value in input.value) {
                        serializeString(value)
                    }
                }
            }
        }
    }
}