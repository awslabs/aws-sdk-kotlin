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
import aws.smithy.kotlin.runtime.serde.xml.XmlUnwrappedOutput

/**
 * Custom deserializer for the  S3 GetBucketLocation operation. This operation does not conform to the model.
 * In the model, there is a nested tag of the same name as the top-level tag, however in practice
 * this child tag is not passed from the service. S3 is sometimes unreliable in how it delivers responses so both
 * possible response types (un-nested and nested) will be considered.
 */
internal fun deserializeGetBucketLocationOperationBody(builder: GetBucketLocationResponse.Builder, payload: ByteArray) {
    deserializeUnwrapped(builder, payload)
    if (builder.locationConstraint == null) deserializeWrapped(builder, payload)
}

private fun deserializeUnwrapped(builder: GetBucketLocationResponse.Builder, payload: ByteArray) {
    val deserializer = XmlDeserializer(payload, true)
    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("LocationConstraint"))
        trait(XmlNamespace("http://s3.amazonaws.com/doc/2006-03-01/"))
        trait(XmlUnwrappedOutput)
    }

    deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
        loop@while (true) {
            when (findNextFieldIndex()) {
                OBJ_DESCRIPTOR.index -> builder.locationConstraint = deserializeString().let { BucketLocationConstraint.fromValue(it) }
                null -> break@loop
                else -> skipValue()
            }
        }
    }
}

private fun deserializeWrapped(builder: GetBucketLocationResponse.Builder, payload: ByteArray) {
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
