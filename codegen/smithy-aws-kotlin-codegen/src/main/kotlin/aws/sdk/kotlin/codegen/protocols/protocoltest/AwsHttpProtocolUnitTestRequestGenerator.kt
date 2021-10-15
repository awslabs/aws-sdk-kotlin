/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.protocoltest

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.middleware.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestRequestGenerator
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
            val staticProviderSymbol = buildSymbol {
                name = "StaticEndpointResolver"
                namespace(AwsKotlinDependency.AWS_CORE, subpackage = "endpoint")
            }
            val endpointSymbol = buildSymbol {
                name = "Endpoint"
                namespace(AwsKotlinDependency.AWS_CORE, subpackage = "endpoint")
            }
            writer.addImport(staticProviderSymbol)
            writer.addImport(endpointSymbol)
            writer.write(
                "endpointResolver = #T(#T(hostname=#S, protocol=#S))",
                staticProviderSymbol, endpointSymbol, expectedHost, "https"
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
        writer.addImport(AwsRuntimeTypes.Types.Credentials)
        writer.write("val credentials = Credentials(#S, #S)", "AKID", "SECRET")
        writer.write("credentialsProvider = StaticCredentialsProvider(credentials)")
    }

    // specify a default region
    writer.write("region = \"us-east-1\"")
}
