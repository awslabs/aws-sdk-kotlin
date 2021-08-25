/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.protocoltest

import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestErrorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestResponseGenerator
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase

/**
 * Override response unit tests to configure AWS specific behaviors
 */
open class AwsHttpProtocolUnitTestResponseGenerator(builder: Builder) : HttpProtocolUnitTestResponseGenerator(builder) {
    override fun renderConfigureServiceClient(test: HttpResponseTestCase) {
        super.renderConfigureServiceClient(test)
        renderConfigureAwsServiceClient(writer, model, serviceShape, operation)
    }

    open class Builder : HttpProtocolUnitTestResponseGenerator.Builder() {
        override fun build(): HttpProtocolUnitTestGenerator<HttpResponseTestCase> =
            AwsHttpProtocolUnitTestResponseGenerator(this)
    }
}

/**
 * Override error unit tests to configure AWS specific behaviors
 */
class AwsHttpProtocolUnitTestErrorGenerator(builder: Builder) : HttpProtocolUnitTestErrorGenerator(builder) {

    override fun renderConfigureServiceClient(test: HttpResponseTestCase) {
        super.renderConfigureServiceClient(test)
        renderConfigureAwsServiceClient(writer, model, serviceShape, operation)
    }

    class Builder : HttpProtocolUnitTestErrorGenerator.Builder() {
        override fun build(): HttpProtocolUnitTestGenerator<HttpResponseTestCase> =
            AwsHttpProtocolUnitTestErrorGenerator(this)
    }
}
