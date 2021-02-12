package com.amazonaws.service.lambda.transform

import com.amazonaws.service.lambda.model.AliasArnType
import com.amazonaws.service.lambda.model.AliasArnType.*
import software.aws.clientrt.serde.*
import software.aws.clientrt.serde.json.JsonSerialName

/**
 * This is a hypothetical type that is not modeled in the Lambda service.  Rather,
 * it's used to demonstrate the Union type in Smithy, for which there is not broad
 * service support at the time of writing this file.
 *
 * If/when there is a Union type modeled in Lambda, this file should be replaced
 * with the actual type(s).
 */
class AliasArnTypeSerializer(val input: AliasArnType) : SdkSerializable {

    companion object {
        private val ALIAS_TYPE_S3_TYPE_FIELD_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("S3ArnType"))
        private val ALIAS_TYPE_EC2_TYPE_FIELD_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Long, JsonSerialName("EC2ArnType"))
        private val ALIAS_TYPE_MULTI_TYPE_FIELD_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Long, JsonSerialName("MultiArnType"))

        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
            JsonSerialName("AliasArnType")
            field(ALIAS_TYPE_S3_TYPE_FIELD_DESCRIPTOR)
            field(ALIAS_TYPE_EC2_TYPE_FIELD_DESCRIPTOR)
        }
    }

    override fun serialize(serializer: Serializer) {
        serializer.serializeStruct(OBJ_DESCRIPTOR) {
            when (input) {
                is S3ArnType -> field(ALIAS_TYPE_S3_TYPE_FIELD_DESCRIPTOR, input.value)
                is EC2ArnType -> field(ALIAS_TYPE_EC2_TYPE_FIELD_DESCRIPTOR, input.value)
                // is MultiArnType -> field(ALIAS_TYPE_MULTI_TYPE_FIELD_DESCRIPTOR, input.value)
            }
        }
    }

}