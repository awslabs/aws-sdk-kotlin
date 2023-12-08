/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.protocoltest.AwsHttpProtocolUnitTestErrorGenerator
import aws.sdk.kotlin.codegen.protocoltest.AwsHttpProtocolUnitTestRequestGenerator
import aws.sdk.kotlin.codegen.protocoltest.AwsHttpProtocolUnitTestResponseGenerator
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointDelegator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*

/**
 * Base class for all AWS SDK HTTP protocol generators
 */
abstract class AwsSdkHttpBindingProtocolGenerator : HttpBindingProtocolGenerator() {

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {
        val ignoredTests = TestMemberDelta(
            setOf(
                // FIXME - compression not yet supported, see https://github.com/awslabs/smithy-kotlin/issues/955
                "SDKAppliedContentEncoding_awsJson1_0",
                "SDKAppliedContentEncoding_awsJson1_1",
                "SDKAppliedContentEncoding_awsQuery",
                "SDKAppliedContentEncoding_ec2Query",
                "SDKAppliedContentEncoding_restJson1",
                "SDKAppliedContentEncoding_restXml",
                "SDKAppendedGzipAfterProvidedEncoding_restJson1",
                "SDKAppendedGzipAfterProvidedEncoding_restXml",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_awsJson1_0",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_awsJson1_1",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_awsQuery",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_ec2Query",
            ),
        )

        // The following can be used to generate only a specific test by name.
        // val targetedTest = TestMemberDelta(setOf("RestJsonComplexErrorWithNoMessage"), TestContainmentMode.RUN_TESTS)

        val requestTestBuilder = AwsHttpProtocolUnitTestRequestGenerator.Builder()
        val responseTestBuilder = AwsHttpProtocolUnitTestResponseGenerator.Builder()
        val errorTestBuilder = AwsHttpProtocolUnitTestErrorGenerator.Builder()

        HttpProtocolTestGenerator(
            ctx,
            requestTestBuilder,
            responseTestBuilder,
            errorTestBuilder,
            ignoredTests,
        ).generateProtocolTests()
    }

    override fun endpointDelegator(ctx: ProtocolGenerator.GenerationContext): EndpointDelegator = AwsEndpointDelegator()
}
