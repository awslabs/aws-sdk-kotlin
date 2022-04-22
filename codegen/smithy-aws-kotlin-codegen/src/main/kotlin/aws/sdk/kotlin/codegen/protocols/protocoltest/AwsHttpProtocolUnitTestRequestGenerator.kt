/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.protocoltest

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestRequestGenerator
import software.amazon.smithy.kotlin.codegen.signing.AwsSignatureVersion4
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
        renderConfigureAwsServiceClient(writer, model, serviceShape, operation)
        test.host.ifPresent { expectedHost ->
            // add an endpoint resolver
            writer.addImport(AwsRuntimeTypes.Endpoint.AwsEndpoint)
            writer.addImport(AwsRuntimeTypes.Endpoint.AwsEndpointResolver)
            writer.write(
                "endpointResolver = #T { _, _ -> #T(#S) }",
                AwsRuntimeTypes.Endpoint.AwsEndpointResolver,
                AwsRuntimeTypes.Endpoint.AwsEndpoint,
                "https://$expectedHost"
            )
        }
    }

    open class Builder : HttpProtocolUnitTestRequestGenerator.Builder() {
        override fun build(): HttpProtocolUnitTestGenerator<HttpRequestTestCase> =
            AwsHttpProtocolUnitTestRequestGenerator(this)
    }
}

internal fun <T : HttpMessageTestCase> HttpProtocolUnitTestGenerator<T>.renderConfigureAwsServiceClient(
    writer: KotlinWriter,
    model: Model,
    serviceShape: ServiceShape,
    operation: OperationShape
) {
    if (AwsSignatureVersion4.hasSigV4AuthScheme(model, serviceShape, operation)) {
        writer.addImport(AwsRuntimeTypes.Config.Credentials.StaticCredentialsProvider)
        writer.addImport(RuntimeTypes.Auth.Credentials.AwsCredentials.Credentials)
        writer.write("val credentials = Credentials(#S, #S)", "AKID", "SECRET")
        writer.write("credentialsProvider = StaticCredentialsProvider(credentials)")
    }

    // specify a default region
    writer.write("region = \"us-east-1\"")
}
