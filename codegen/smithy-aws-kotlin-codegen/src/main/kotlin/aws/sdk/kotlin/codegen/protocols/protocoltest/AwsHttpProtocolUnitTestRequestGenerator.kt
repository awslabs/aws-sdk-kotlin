/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.protocoltest

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointProviderGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestRequestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait

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
    if (AwsSignatureVersion4.hasSigV4AuthScheme(model, serviceShape, operation)) {
        writer.addImport(AwsRuntimeTypes.Config.Credentials.StaticCredentialsProvider)
        writer.addImport(RuntimeTypes.Auth.Credentials.AwsCredentials.Credentials)
        writer.write("val credentials = Credentials(#S, #S)", "AKID", "SECRET")
        writer.write("credentialsProvider = StaticCredentialsProvider(credentials)")
    }

    // specify a default region
    writer.write("region = \"us-east-1\"")

    // if the model doesn't have endpoint rules we have to fill it in with a default
    if (!serviceShape.hasTrait<EndpointRuleSetTrait>()) {
        writer.write(
            "endpointProvider = #T { #T(#S) }",
            EndpointProviderGenerator.getSymbol(ctx.settings),
            RuntimeTypes.SmithyClient.Endpoints.Endpoint,
            "https://$hostname",
        )
    }
}
