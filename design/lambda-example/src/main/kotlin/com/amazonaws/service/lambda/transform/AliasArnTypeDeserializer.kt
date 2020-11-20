package com.amazonaws.service.lambda.transform

import com.amazonaws.service.lambda.model.AliasArnType
import com.amazonaws.service.lambda.model.AliasArnType.EC2ArnType
import com.amazonaws.service.lambda.model.AliasArnType.MultiArnType
import com.amazonaws.service.lambda.model.AliasArnType.S3ArnType
import software.aws.clientrt.serde.*

/**
 * This is a hypothetical type that is not modeled in the Lambda service.  Rather,
 * it's used to demonstrate the Union type in Smithy, for which there is not broad
 * service support at the time of writing this file.
 *
 * If/when there is a Union type modeled in Lambda, this file should be replaced
 * with the actual type(s).
 */
class AliasArnTypeDeserializer {

    companion object {
        private val ALIAS_S3_ARN_FIELD_DESCRIPTOR = SdkFieldDescriptor("S3AliasArn", SerialKind.String)
        private val ALIAS_EC2_ARN_FIELD_DESCRIPTOR = SdkFieldDescriptor("EC2AliasArn", SerialKind.Integer)
        private val ALIAS_MULTI_ARN_FIELD_DESCRIPTOR = SdkFieldDescriptor("MultiAliasArn", SerialKind.Integer)

        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build() {
            serialName = "AliasArn"
            field(ALIAS_S3_ARN_FIELD_DESCRIPTOR)
            field(ALIAS_EC2_ARN_FIELD_DESCRIPTOR)
        }

        fun deserialize(deserializer: Deserializer): AliasArnType? {
            var aliasArnType: AliasArnType? = null

            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                when(findNextFieldIndex()) {
                    ALIAS_S3_ARN_FIELD_DESCRIPTOR.index -> aliasArnType = deserializeString()?.let { S3ArnType(it) }
                    ALIAS_EC2_ARN_FIELD_DESCRIPTOR.index -> aliasArnType = deserializeInt()?.let { EC2ArnType(it) }
                    ALIAS_MULTI_ARN_FIELD_DESCRIPTOR.index -> aliasArnType = deserializer.deserializeList(ALIAS_MULTI_ARN_FIELD_DESCRIPTOR) {
                        val list0 = mutableListOf<String>()
                        while(hasNextElement()) {
                            list0.add(deserializeString()!!)
                        }
                        MultiArnType(list0)
                    }
                    else -> null
                }
            }

            return aliasArnType
        }
    }
}