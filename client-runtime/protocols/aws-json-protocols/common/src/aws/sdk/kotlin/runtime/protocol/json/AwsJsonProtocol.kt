/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.json

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
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
public class AwsJsonProtocol(config: Config) : Feature {
    private val serviceShapeName: String = requireNotNull(config.serviceShapeName) { "AWS JSON protocol service name must be specified" }
    private val version: String = requireNotNull(config.version) { "AWS JSON protocol version must be specified" }

    public class Config {
        /**
         * The original service (shape) name
         */
        public var serviceShapeName: String? = null

        /**
         * The protocol version e.g. "1.0"
         */
        public var version: String? = null
    }

    public companion object Feature : HttpClientFeatureFactory<Config, AwsJsonProtocol> {
        override val key: FeatureKey<AwsJsonProtocol> = FeatureKey("AwsJsonProtocol")

        override fun create(block: Config.() -> Unit): AwsJsonProtocol {
            val config = Config().apply(block)
            return AwsJsonProtocol(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.mutate.intercept { req, next ->
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

            next.call(req)
        }
    }
}
