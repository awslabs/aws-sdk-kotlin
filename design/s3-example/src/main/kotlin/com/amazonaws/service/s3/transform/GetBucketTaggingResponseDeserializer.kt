/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.service.s3.transform

import com.amazonaws.service.s3.model.GetBucketTaggingResponse
import com.amazonaws.service.s3.model.Tag
import software.aws.clientrt.http.feature.DeserializationProvider
import software.aws.clientrt.http.feature.HttpDeserialize
import software.aws.clientrt.http.readAll
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.serde.*
import software.aws.clientrt.serde.xml.XmlList

class GetBucketTaggingResponseDeserializer : HttpDeserialize {

    companion object {
        private val TAG_SET_FIELD_DESCRIPTOR =
            SdkFieldDescriptor("TagSet", SerialKind.List, 0, XmlList(elementName = "Tag"))

        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build() {
            serialName = "Tagging"
            field(TAG_SET_FIELD_DESCRIPTOR)
        }
    }

    override suspend fun deserialize(
        response: HttpResponse,
        provider: DeserializationProvider
    ): GetBucketTaggingResponse {
        val builder = GetBucketTaggingResponse.builder()
        val body = response.body.readAll()!!
        println(String(body))
        val deserializer = provider(body)

        deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
            loop@ while (true) {
                when (findNextFieldIndex()) {
                    TAG_SET_FIELD_DESCRIPTOR.index -> builder.tagSet =
                        deserializer.deserializeList(TAG_SET_FIELD_DESCRIPTOR) {
                            val list = mutableListOf<Tag>()
                            while (hasNextElement()) {
                                list.add(Tag.deserialize(deserializer))
                            }
                            return@deserializeList list
                        }
                    null -> break@loop
                    else -> skipValue()
                }
            }
        }
        return builder.build()
    }
}