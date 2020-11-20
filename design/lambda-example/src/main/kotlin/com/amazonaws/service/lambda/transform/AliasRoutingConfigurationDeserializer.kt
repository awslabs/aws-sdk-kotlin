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

import com.amazonaws.service.lambda.model.AliasRoutingConfiguration
import software.aws.clientrt.serde.*


// TODO: Decision needed: only HttpSerialize/HttpDeserialize for operation inputs/outputs. Here this type is only
// used as a child field of an operations output (CreateAlias), it is not used directly in the operation output and
// therefore none AliasRoutingConfiguration fields can come from the HTTP protocol details (AliasConfiguration fields
// can but this type itself cannot)
class AliasRoutingConfigurationDeserializer {
    companion object {
        private val ADDITIONAL_VERSION_WEIGHTS_FIELD_DESCRIPTOR = SdkFieldDescriptor("AdditionalVersionWeights", SerialKind.Map)

        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build() {
            field(ADDITIONAL_VERSION_WEIGHTS_FIELD_DESCRIPTOR)
        }

        fun deserialize(deserializer: Deserializer): AliasRoutingConfiguration {
            val builder = AliasRoutingConfiguration.dslBuilder()

            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while(true) {
                    when(findNextFieldIndex()) {
                        ADDITIONAL_VERSION_WEIGHTS_FIELD_DESCRIPTOR.index -> builder.additionalVersionWeights = deserializer.deserializeMap(ADDITIONAL_VERSION_WEIGHTS_FIELD_DESCRIPTOR) {
                            val map = mutableMapOf<String, Float?>()
                            while(hasNextEntry()) {
                                map[key()] = deserializeFloat()
                            }
                            return@deserializeMap map
                        }
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            return builder.build()
        }
    }
}
