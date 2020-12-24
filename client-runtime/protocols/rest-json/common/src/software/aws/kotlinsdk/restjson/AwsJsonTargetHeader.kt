/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.aws.kotlinsdk.restjson

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
 *
 * TODO ~ enable this type to handle both awsJson1_0 and awsJson1_1 protocols.
 */
@InternalAPI
public class AwsJsonTargetHeader() : Feature {

    public companion object Feature : HttpClientFeatureFactory<Unit, AwsJsonTargetHeader> {
        override val key: FeatureKey<AwsJsonTargetHeader> = FeatureKey("AwsJsonTargetHeader")
        override fun create(block: Unit.() -> Unit): AwsJsonTargetHeader {
            return AwsJsonTargetHeader()
        }
    }

    override fun install(client: SdkHttpClient) {
        client.requestPipeline.intercept(HttpRequestPipeline.Initialize) {
            val serviceName = this.context.executionContext[SdkClientOption.ServiceName]
            val operationName = this.context.executionContext[SdkClientOption.OperationName]

            // TODO - associate this operation a link to future awsJson1_0 spec.
            this.subject.headers.append("X-Amz-Target", "$serviceName.$operationName")

            // TODO - in the case of awsJson requests without inputs, there is no serializer associated w/ the operation
            //  As-is the serializer is responsible for populating the Content-Type header.  This may change
            //  when synthetic inputs are added such that all operations have inputs.  Readdress this issue after that work
            //  is complete.
            if (!this.subject.headers.contains("Content-Type")) {
                subject.headers.append("Content-Type", "application/x-amz-json-1.0")
            }
        }
    }
}
