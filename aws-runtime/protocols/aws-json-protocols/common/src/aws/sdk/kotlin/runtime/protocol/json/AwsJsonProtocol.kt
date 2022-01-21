/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.json

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.operation.ModifyRequestMiddleware
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
import aws.smithy.kotlin.runtime.util.get

/**
 * Http feature that handles AWS JSON protocol behaviors, see:
 *   - https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_0-protocol.html
 *   - https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_1-protocol.html
 *
 * Including:
 *   - setting the `Content-Type` and `X-Amz-Target` headers
 *   - providing an empty json {} body when no body is serialized
 */
@InternalSdkApi
public class AwsJsonProtocol(
    /**
     * The original service (shape) name
     */
    private val serviceShapeName: String,

    /**
     * The protocol version e.g. "1.0"
     */
    private val version: String
) : ModifyRequestMiddleware {

    override suspend fun modifyRequest(req: SdkHttpRequest): SdkHttpRequest {
        val context = req.context
        // required context elements
        val operationName = context[SdkClientOption.OperationName]

        // see: https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_0-protocol.html#protocol-behaviors
        req.subject.headers.append("X-Amz-Target", "$serviceShapeName.$operationName")
        req.subject.headers.setMissing("Content-Type", "application/x-amz-json-$version")

        if (req.subject.body is HttpBody.Empty) {
            // Empty body is required by AWS JSON 1.x protocols
            // https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_0-protocol.html#empty-body-serialization
            // https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_1-protocol.html#empty-body-serialization
            req.subject.body = ByteArrayContent("{}".encodeToByteArray())
        }
        return req
    }
}
