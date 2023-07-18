/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.GetBucketLocationResponse
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.awsprotocol.withPayload
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.isSuccess
import aws.smithy.kotlin.runtime.http.operation.HttpDeserialize
import aws.smithy.kotlin.runtime.http.readAll
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.serde.xml.dom.parseDom
import aws.smithy.kotlin.runtime.serde.xml.xmlStreamReader

/**
 * Custom deserializer for the GetBucketLocation operation.  This operation does not conform to the model.
 * In the model, there is a nested tag of the same name as the top-level tag, however in practice
 * this child tag is not passed from the service.  In this implementation the model is not used for
 * deserialization.
 */
internal class GetBucketLocationOperationDeserializer : HttpDeserialize<GetBucketLocationResponse> {

    override suspend fun deserialize(context: ExecutionContext, response: HttpResponse): GetBucketLocationResponse {
        if (!response.status.isSuccess()) {
            throwGetBucketLocationError(context, response)
        }
        val builder = GetBucketLocationResponse.Builder()

        val payload = response.body.readAll()
        if (payload != null) {
            deserializeGetBucketLocationOperationBody(builder, payload)
        }
        return builder.build()
    }
}

private suspend fun throwGetBucketLocationError(context: ExecutionContext, response: HttpResponse): kotlin.Nothing {
    val payload = response.body.readAll()
    val wrappedResponse = response.withPayload(payload)

    val errorDetails = try {
        if (payload == null && response.status == HttpStatusCode.NotFound) {
            S3ErrorDetails(code = "NotFound")
        } else {
            checkNotNull(payload){ "unable to parse error from empty response" }
            parseS3ErrorResponse(payload)
        }
    } catch (ex: Exception) {
        throw S3Exception("Failed to parse response as 'restXml' error", ex).also {
            setS3ErrorMetadata(it, wrappedResponse, null)
        }
    }

    val ex = when(errorDetails.code) {
        else -> S3Exception(errorDetails.message)
    }

    setS3ErrorMetadata(ex, wrappedResponse, errorDetails)
    throw ex
}

private fun deserializeGetBucketLocationOperationBody(builder: GetBucketLocationResponse.Builder, payload: ByteArray) {
    val dom = parseDom(xmlStreamReader(payload))
    check(dom.name.local == "LocationConstraint") { "Expected top-level tag of 'LocationConstraint' but found ${dom.name}." }
    if (dom.children.isEmpty()) {
        val rawLocationConstraint = checkNotNull(dom.text) { "Did not receive a value for 'LocationConstraint' in response." }
        builder.locationConstraint = BucketLocationConstraint.fromValue(rawLocationConstraint)
    } else {
        check(dom.children["LocationConstraint"]?.get(0)?.name?.local == "LocationConstraint") { "Expected second-level tag of 'LocationConstraint' but found ${dom.name}." }
        val rawLocationConstraint = checkNotNull(dom.children["LocationConstraint"]?.get(0)?.text) { "Did not receive a value for 'LocationConstraint' in response." }
        builder.locationConstraint = BucketLocationConstraint.fromValue(rawLocationConstraint)
    }
}
