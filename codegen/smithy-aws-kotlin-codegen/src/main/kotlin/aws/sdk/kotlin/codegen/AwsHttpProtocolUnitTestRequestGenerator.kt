/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.integration.HttpProtocolUnitTestGenerator
import software.amazon.smithy.kotlin.codegen.integration.HttpProtocolUnitTestRequestGenerator
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase

/**
 * Override request unit tests to configure AWS specific behaviors
 */
class AwsHttpProtocolUnitTestRequestGenerator(builder: Builder) :
    HttpProtocolUnitTestRequestGenerator(builder) {
    override fun renderConfigureServiceClient(test: HttpRequestTestCase) {
        super.renderConfigureServiceClient(test)
        // specify a default region
        writer.write("region = \"us-east-1\"")
    }

    open class Builder : HttpProtocolUnitTestRequestGenerator.Builder() {
        override fun build(): HttpProtocolUnitTestGenerator<HttpRequestTestCase> {
            return AwsHttpProtocolUnitTestRequestGenerator(this)
        }
    }
}
