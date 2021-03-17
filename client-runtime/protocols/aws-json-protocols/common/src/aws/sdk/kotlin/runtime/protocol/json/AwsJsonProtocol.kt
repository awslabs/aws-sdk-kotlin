/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.json

import software.aws.clientrt.client.SdkClientOption
import software.aws.clientrt.http.*
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.util.InternalApi
import software.aws.clientrt.util.get

/**
 * Http feature that handles AWS JSON protocol behaviors, see:
 *   - https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_0-protocol.html
 *   - https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_1-protocol.html
 *
 * Including:
 *   - setting the `Content-Type` and `X-Amz-Target` headers
 *   - providing an empty json {} body when no body is serialized
 */
@InternalApi
public class AwsJsonProtocol(config: Config) : Feature {
    private val version: String = requireNotNull(config.version) { "AWS JSON Protocol version must be specified" }

    public class Config {
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
            val serviceName = context[SdkClientOption.ServiceName]
            val operationName = context[SdkClientOption.OperationName]

            // see: https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_0-protocol.html#protocol-behaviors
            req.subject.headers.append("X-Amz-Target", "$serviceName.$operationName")
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
