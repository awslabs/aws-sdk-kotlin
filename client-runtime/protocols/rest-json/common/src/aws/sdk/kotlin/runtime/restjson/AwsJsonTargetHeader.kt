/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.restjson

import software.aws.clientrt.client.SdkClientOption
import software.aws.clientrt.http.Feature
import software.aws.clientrt.http.FeatureKey
import software.aws.clientrt.http.HttpClientFeatureFactory
import software.aws.clientrt.http.SdkHttpClient
import software.aws.clientrt.http.request.HttpRequestPipeline
import software.aws.clientrt.util.InternalAPI
import software.aws.clientrt.util.get

/**
 * Http feature that populates the target header for awsJson protocol-based requests.
 */
@InternalAPI
public class AwsJsonTargetHeader(config: Config) : Feature {
    private val version: String = requireNotNull(config.version) { "AWS JSON Protocol version must be specified" }

    public class Config {
        /**
         * The protocol version e.g. "1.0"
         */
        public var version: String? = null
    }

    public companion object Feature : HttpClientFeatureFactory<Config, AwsJsonTargetHeader> {
        override val key: FeatureKey<AwsJsonTargetHeader> = FeatureKey("AwsJsonTargetHeader")
        override fun create(block: Config.() -> Unit): AwsJsonTargetHeader {
            val config = Config().apply(block)
            return AwsJsonTargetHeader(config)
        }
    }

    override fun install(client: SdkHttpClient) {
        client.requestPipeline.intercept(HttpRequestPipeline.Initialize) {
            val serviceName = context.executionContext[SdkClientOption.ServiceName]
            val operationName = context.executionContext[SdkClientOption.OperationName]

            // TODO - associate this operation a link to future awsJson1_0 spec.
            subject.headers.append("X-Amz-Target", "$serviceName.$operationName")

            // TODO - in the case of awsJson requests without inputs, there is no serializer associated w/ the operation
            //  As-is the serializer is responsible for populating the Content-Type header.  This may change
            //  when synthetic inputs are added such that all operations have inputs.  Readdress this issue after that work
            //  is complete.
            if (!subject.headers.contains("Content-Type")) {
                subject.headers.append("Content-Type", "application/x-amz-json-$version")
            }
        }
    }
}
