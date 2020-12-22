/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.integration.HttpProtocolUnitTestErrorGenerator
import software.amazon.smithy.kotlin.codegen.integration.HttpProtocolUnitTestGenerator
import software.amazon.smithy.kotlin.codegen.integration.HttpProtocolUnitTestResponseGenerator
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase

/**
 * Override response unit tests to configure AWS specific behaviors
 */
open class AwsHttpProtocolUnitTestResponseGenerator(builder: Builder) : HttpProtocolUnitTestResponseGenerator(builder) {
    override fun renderConfigureServiceClient(test: HttpResponseTestCase) {
        super.renderConfigureServiceClient(test)
        // specify a default region
        writer.write("region = \"us-east-1\"")
    }

    open class Builder : HttpProtocolUnitTestResponseGenerator.Builder() {
        override fun build(): HttpProtocolUnitTestGenerator<HttpResponseTestCase> {
            return AwsHttpProtocolUnitTestResponseGenerator(this)
        }
    }
}

/**
 * Override error unit tests to configure AWS specific behaviors
 */
class AwsHttpProtocolUnitTestErrorGenerator(builder: Builder) : HttpProtocolUnitTestErrorGenerator(builder) {

    override fun renderConfigureServiceClient(test: HttpResponseTestCase) {
        super.renderConfigureServiceClient(test)
        // specify a default region
        writer.write("region = \"us-east-1\"")
    }

    class Builder : HttpProtocolUnitTestErrorGenerator.Builder() {
        override fun build(): HttpProtocolUnitTestGenerator<HttpResponseTestCase> {
            return AwsHttpProtocolUnitTestErrorGenerator(this)
        }
    }
}
