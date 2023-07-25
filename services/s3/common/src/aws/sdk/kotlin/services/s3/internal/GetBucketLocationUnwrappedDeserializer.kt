/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.GetBucketLocationResponse
import aws.smithy.kotlin.runtime.serde.SdkFieldDescriptor
import aws.smithy.kotlin.runtime.serde.SdkObjectDescriptor
import aws.smithy.kotlin.runtime.serde.SerialKind
import aws.smithy.kotlin.runtime.serde.deserializeStruct
import aws.smithy.kotlin.runtime.serde.xml.XmlDeserializer
import aws.smithy.kotlin.runtime.serde.xml.XmlNamespace
import aws.smithy.kotlin.runtime.serde.xml.XmlSerialName

internal fun deserializeWrapped(builder: GetBucketLocationResponse.Builder, payload: ByteArray) {
    val deserializer = XmlDeserializer(payload, true)
    val LOCATIONCONSTRAINT_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Enum, XmlSerialName("LocationConstraint"))
    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("LocationConstraint"))
        trait(XmlNamespace("http://s3.amazonaws.com/doc/2006-03-01/"))
        field(LOCATIONCONSTRAINT_DESCRIPTOR)
    }

    deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
        loop@while (true) {
            when (findNextFieldIndex()) {
                LOCATIONCONSTRAINT_DESCRIPTOR.index -> builder.locationConstraint = deserializeString().let { BucketLocationConstraint.fromValue(it) }
                null -> break@loop
                else -> skipValue()
            }
        }
    }
}
