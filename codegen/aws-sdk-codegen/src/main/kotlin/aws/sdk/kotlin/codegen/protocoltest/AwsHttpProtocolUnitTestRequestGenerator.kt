/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocoltest

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestRequestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase

/**
 * Override request unit tests to configure AWS specific behaviors
 */
class AwsHttpProtocolUnitTestRequestGenerator(builder: Builder) :
    HttpProtocolUnitTestRequestGenerator(builder) {
    override fun renderConfigureServiceClient(test: HttpRequestTestCase) {
        super.renderConfigureServiceClient(test)
        renderConfigureAwsServiceClient(ctx, writer, model, serviceShape, operation, test.host.getOrNull() ?: "hostname")
    }

    open class Builder : HttpProtocolUnitTestRequestGenerator.Builder() {
        override fun build(): HttpProtocolUnitTestGenerator<HttpRequestTestCase> =
            AwsHttpProtocolUnitTestRequestGenerator(this)
    }
}

internal fun <T : HttpMessageTestCase> HttpProtocolUnitTestGenerator<T>.renderConfigureAwsServiceClient(
    ctx: ProtocolGenerator.GenerationContext,
    writer: KotlinWriter,
    model: Model,
    serviceShape: ServiceShape,
    operation: OperationShape,
    hostname: String = "hostname",
) {
    // specify a default region
    writer.write("region = \"us-east-1\"")
}
